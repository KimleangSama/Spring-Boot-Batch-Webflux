package com.keakimleang.springbatchwebflux.services;

import com.keakimleang.springbatchwebflux.batches.*;
import static com.keakimleang.springbatchwebflux.batches.consts.BatchFieldName.*;
import com.keakimleang.springbatchwebflux.entities.*;
import com.keakimleang.springbatchwebflux.payloads.*;
import com.keakimleang.springbatchwebflux.repos.*;
import com.keakimleang.springbatchwebflux.utils.*;
import java.io.*;
import java.nio.file.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.*;
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
    private final JobLauncher jobLauncher;
    private final Job uploadBatchJob;

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
                    .setStatus("Processing")
                    .setTotalRecords(0)
                    .setValidRecords(0)
                    .setInvalidRecords(0);
            return batchUploadRepository.save(batchUpload);
        });
    }

}
