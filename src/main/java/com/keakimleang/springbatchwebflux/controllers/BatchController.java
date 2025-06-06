package com.keakimleang.springbatchwebflux.controllers;

import com.keakimleang.springbatchwebflux.entities.*;
import com.keakimleang.springbatchwebflux.payloads.*;
import com.keakimleang.springbatchwebflux.services.*;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
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
    public Mono<ResponseEntity<ApiResponse<Map<String, Long>>>> confirm(@PathVariable("batchUploadId") final Long billUploadId) {
        return batchService.confirm(billUploadId)
                .map(data -> ResponseEntity.ok(ApiResponse.cratedResourceResponse(data, "Biller uploaded successfully!")));
    }

    @GetMapping(value = "{batchUploadId}/records")
    public Mono<ResponseEntity<ApiResponse<List<BatchUploadProd>>>> getRecords(
            @PathVariable("batchUploadId") final Long batchUploadId) {
        return batchService.getBatchRecordsByBatchUploadId(batchUploadId)
                .collectList()
                .map(data -> ResponseEntity.ok(ApiResponse.okResponse(data, null)));
    }
}
