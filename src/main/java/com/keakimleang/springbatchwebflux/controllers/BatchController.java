package com.keakimleang.springbatchwebflux.controllers;

import co.elastic.clients.elasticsearch.core.search.*;
import com.keakimleang.springbatchwebflux.entities.*;
import com.keakimleang.springbatchwebflux.payloads.*;
import com.keakimleang.springbatchwebflux.services.*;
import com.keakimleang.springbatchwebflux.utils.*;
import java.io.*;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;
import reactor.core.scheduler.*;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
@Slf4j
public class BatchController {
    private final BatchService batchService;
    private final ElasticsearchService elasticsearchService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<Map<String, Long>>>> upload(
            @RequestPart("file") final Mono<FilePart> filePartMono,
            @RequestPart("batchOwnerName") final Mono<String> batchOwnerNameMono) {
        return batchService
                .upload(Mono.zip(filePartMono, batchOwnerNameMono)
                        .map(tuple -> new BatchUploadRequest(tuple.getT1(), tuple.getT2(), false)))
                .map(batchId -> ResponseEntity.ok(ApiResponse.cratedResourceResponse(batchId, null)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping(value = "{batchUploadId}/confirm")
    public Mono<ResponseEntity<ApiResponse<Map<String, Long>>>> confirm(@PathVariable("batchUploadId") final Long batchUploadId) {
        return batchService.confirm(batchUploadId)
                .map(data -> ResponseEntity.ok(ApiResponse.cratedResourceResponse(data, "Batcher uploaded successfully!")));
    }

    @GetMapping(value = "{batchUploadId}/records")
    public Mono<ResponseEntity<ApiResponse<List<BatchUploadProd>>>> getRecords(
            @PathVariable("batchUploadId") final Long batchUploadId) {
        return batchService.getBatchRecordsByBatchUploadId(batchUploadId)
                .collectList()
                .map(data -> ResponseEntity.ok(ApiResponse.okResponse(data, null)));
    }

    @PostMapping(value = "/records/filter")
    public Mono<ResponseEntity<ApiResponse<List<BatchUploadProd>>>> getRecordsByFilter(
            @RequestBody final BatchUploadRecordFilterRequest request) {
        return batchService.getBatchRecordsByFilter(request)
                .collectList()
                .map(data -> ResponseEntity.ok(ApiResponse.okResponse(data, null)));
    }

    @PostMapping("/migration")
    public Mono<ResponseEntity<String>> migrateToElasticsearch() {
        // Migrate the data to Elasticsearch without waiting for it to complete
        elasticsearchService.migrateDataToElasticsearch(); // Trigger in background
        return Mono.just(ResponseEntity.ok("Migration started successfully"));
    }

    @GetMapping(value = "/fuzzy-search")
    public Mono<ResponseEntity<ApiResponse<List<BatchUploadProdElasticsearch>>>> fuzzySearch(
            @RequestParam("searchTerm") final String searchTerm) throws IOException {
        return elasticsearchService.fuzzySearchCustomerCode(searchTerm)
                .map(response -> {
                    final List<BatchUploadProdElasticsearch> results = response.hits().hits().stream()
                            .map(Hit::source)
                            .toList();
                    return ResponseEntity.ok(ApiResponse.okResponse(results, "Search results found: " + results.size()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/{batchUploadId}/excel/export", produces = MediaTypeConstant.EXCEL)
    public Mono<ResponseEntity<Resource>> exportToExcel(@PathVariable("batchUploadId") final Long batchUploadId) {
        return batchService.downloadExcelBatchRecords(batchUploadId)
                .map(fileDownload -> ResponseEntity.ok()
                        .headers(fileDownload.headers())
                        .body(fileDownload.file()));
    }
}
