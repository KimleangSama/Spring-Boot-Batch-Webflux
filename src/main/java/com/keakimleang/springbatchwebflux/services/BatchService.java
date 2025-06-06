package com.keakimleang.springbatchwebflux.services;

import com.fasterxml.jackson.databind.*;
import com.keakimleang.springbatchwebflux.annotations.*;
import com.keakimleang.springbatchwebflux.batches.*;
import static com.keakimleang.springbatchwebflux.batches.consts.BatchFieldName.*;
import com.keakimleang.springbatchwebflux.entities.*;
import com.keakimleang.springbatchwebflux.payloads.*;
import com.keakimleang.springbatchwebflux.repos.*;
import com.keakimleang.springbatchwebflux.utils.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.*;
import org.springframework.data.r2dbc.core.*;
import org.springframework.r2dbc.core.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;
import reactor.core.scheduler.*;
import reactor.util.function.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class BatchService {
    private final BatchUploadValidator batchUploadValidator;
    private final BatchUploadRepository batchUploadRepository;
    private final BatchUploadStagingRepository batchUploadStagingRepository;
    private final BatchUploadProdRepository batchUploadProdRepository;
    private final JobLauncher jobLauncher;
    private final Job uploadBatchJob;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;

    public Mono<Long> upload(final Mono<BatchUploadRequest> requestMono) {
        return requestMono
                .flatMap(batchUploadValidator::validateFile)
                .flatMap(request -> saveUploadToTempDir(request)
                        .flatMap(temp -> Mono.zip(Mono.just(request), Mono.just(temp))))
                .flatMap(tuple2 -> Mono.zip(Mono.just(tuple2.getT2()), saveBillUpload(tuple2.getT1()), Mono.just(tuple2.getT1())))
                .flatMap(tuple3 -> {
                    final var runAsync = tuple3.getT3().runJobAsync();
                    if (runAsync) {
                        // Run the job in the background and immediately return the upload ID
                        Mono.fromRunnable(() -> processBillUploadJob(tuple3, true).subscribe())
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                        // Return the upload ID immediately without waiting for job completion
                        return Mono.just(tuple3.getT2().getId());
                    } else {
                        return processBillUploadJob(tuple3, false);
                    }
                });
    }

    private Mono<Long> processBillUploadJob(final Tuple2<Path, BatchUpload> tuple2,
                                            final boolean runJobAsync) {
        return Mono.fromCallable(() -> {
                    final var jobParams = new JobParametersBuilder()
                            .addLong(UPLOAD_BATCH_ID, tuple2.getT2().getId())
                            .addString(UPLOAD_FILE, tuple2.getT1().toAbsolutePath().toString())
                            .addString(RUN_ASYNC_FLOW, String.valueOf(runJobAsync))
                            .toJobParameters();
                    final var job = jobLauncher.run(uploadBatchJob, jobParams);
                    if (!BatchStatus.COMPLETED.equals(job.getStatus())) {
                        log.info("batchUploadId {} process by jobId {} failed with status={} and existStatus={}",
                                tuple2.getT2().getId(), job.getJobId(), job.getStatus(), job.getExitStatus().getExitCode());
                        final var msg = "Fail to upload batcher due to " + job.getAllFailureExceptions().getFirst().getMessage();
                        throw new BatchJobFailedException(tuple2.getT2().getId(), msg);
                    }
                    return tuple2.getT2().getId();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    // Keep record for user confirm if run async
                    if (runJobAsync) {
                        return Mono.error(e);
                    } else {
                        // Rollback previous save record
                        final var jobError = (BatchJobFailedException) e;
                        final var rollbackStaging = batchUploadStagingRepository.deleteByBatchUploadId(jobError.getBatchUploadId());
                        final var rollBackBatchUpload = batchUploadRepository.deleteById(jobError.getBatchUploadId());
                        return Mono.when(rollbackStaging, rollBackBatchUpload)
                                .then(Mono.error(e));
                    }
                });
    }

    private Mono<Path> saveUploadToTempDir(final BatchUploadRequest request) {
        return Mono.fromCallable(() -> {
                    final var filePart = request.file();
                    final var ext = "." + AppUtil.rsplit(filePart.filename(), ".", 2).getLast();
                    return Files.createTempFile("batch", ext);
                })
                .subscribeOn(Schedulers.boundedElastic()) // Run blocking call on a bounded elastic thread
                .flatMap(temp -> request.file().transferTo(temp).thenReturn(temp))
                .onErrorMap(IOException.class, RuntimeException::new); // Wrap IOException into unchecked
    }

    private Mono<BatchUpload> saveBillUpload(final BatchUploadRequest request) {
        return Mono.defer(() -> {
            final var now = DateUtil.now();
            final var batchUpload = new BatchUpload()
                    .setUploadedAt(now)
                    .setBatchOwnerName(request.batchOwnerName())
                    .setStatus("PROCESSING")
                    .setTotalRecords(0)
                    .setValidRecords(0)
                    .setInvalidRecords(0);
            return batchUploadRepository.save(batchUpload);
        });
    }

    @ReactiveTransaction
    public Mono<Long> confirm(Long batchUploadId) {
        return batchUploadRepository.findById(batchUploadId)
                .switchIfEmpty(Mono.error(new BatchServiceException("Batch upload not found with ID: " + batchUploadId)))
                .flatMap(batchUpload -> {
                    if (!"AWAIT_CONFIRM".equalsIgnoreCase(batchUpload.getStatus())) {
                        return Mono.error(new BatchServiceException(
                                "Batch upload with ID " + batchUploadId + " is not in a valid state for confirmation. Current status: " + batchUpload.getStatus()));
                    }
                    return batchUploadProdRepository.moveValidRecordFromStagingToProd(batchUploadId)
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(movedCount -> {
                                if (movedCount <= 0) {
                                    return Mono.error(new BatchServiceException(
                                            "No valid records found to move for batch upload ID: " + batchUploadId));
                                }
                                log.info("Moved {} valid records from staging to production for batch upload ID: {}", movedCount, batchUploadId);
                                return batchUploadStagingRepository.deleteByBatchUploadId(batchUploadId)
                                        .then(batchUploadProdRepository.findByBatchUploadId(batchUploadId).collectList())
                                        .flatMap(records -> {
                                            String redisKey = "batchUploadId:" + batchUploadId;
                                            return redisCacheService.setListValue(redisKey, records, Duration.ofHours(1))
                                                    .onErrorResume(e -> {
                                                        log.warn("Failed to cache batch upload ID {} to Redis: {}", batchUploadId, e.getMessage());
                                                        return Mono.empty();
                                                    });
                                        })
                                        .then(Mono.defer(() -> {
                                            batchUpload.setStatus("CONFIRM");
                                            batchUpload.setTotalRecords(movedCount.intValue());
                                            batchUpload.setValidRecords(movedCount.intValue());
                                            batchUpload.setInvalidRecords(0);
                                            batchUpload.setSubmittedAt(DateUtil.now());

                                            return batchUploadRepository.save(batchUpload)
                                                    .doOnSuccess(saved -> log.info("BatchUpload ID {} processed successfully and sending to message broker", saved.getId()))
                                                    .thenReturn(batchUpload.getId());
                                        }));
                            });
                });
    }

    @ReactiveTransaction(readOnly = true)
    public Flux<BatchUploadProd> getBatchRecordsByBatchUploadId(Long batchUploadId) {
        return redisCacheService.getListValue("batchUploadId:" + batchUploadId)
                .map(obj -> objectMapper.convertValue(obj, BatchUploadProd.class))
                .switchIfEmpty(batchUploadProdRepository.findByBatchUploadId(batchUploadId)
                        .doOnNext(record -> log.info("Cache miss for batch upload ID {}, fetching from database", batchUploadId)))
                .cast(BatchUploadProd.class)
                .doOnError(e -> log.error("Error retrieving records for batch upload ID {}: {}", batchUploadId, e.getMessage()));
    }

    @ReactiveTransaction(readOnly = true)
    public Flux<BatchUploadProd> getBatchRecordsByFilter(BatchUploadRecordFilterRequest request) {
        String sql = """
                    SELECT bp.*
                    FROM batches_uploads_prod bp
                    JOIN batches_uploads bu ON bp.batch_upload_id = bu.id
                    WHERE (:batchUploadId IS NULL OR bp.batch_upload_id = :batchUploadId)
                      AND (:batchOwnerName IS NULL OR bu.batch_owner_name = :batchOwnerName)
                """;
        return databaseClient.sql(sql)
                .bind("batchUploadId", request.getBatchUploadId())
                .bind("batchOwnerName", request.getBatchOwnerName())
                .map((row, metadata) -> new BatchUploadProd(
                        row.get("id", Long.class),
                        row.get("batch_upload_id", Long.class),
                        row.get("customer_code", String.class),
                        row.get("invoice_date", LocalDate.class),
                        row.get("due_amount", BigDecimal.class),
                        row.get("currency", String.class),
                        row.get("created_at", LocalDateTime.class)
                ))
                .all()
                .doOnNext(record -> log.info("Fetched joined record: {}", record));
    }

//    @ReactiveTransaction(readOnly = true)
//    public Flux<BatchUploadProd> getBatchRecordsByFilter(BatchUploadRecordFilterRequest request) {
//        final var filter = new BatchUploadRecordFilter();
//        filter.setBatchUploadId(request.getBatchUploadId());
//        filter.setBatchOwnerName(request.getBatchOwnerName());
//        return Flux.defer(() -> {
//            final var criteria = filter.getCriteria();
//            final Query query = Query.query(criteria);
//            return template.select(query, BatchUploadProd.class)
//                    .doOnNext(record -> log.info("Fetched record: {}", record));
//        });
//    }

}
