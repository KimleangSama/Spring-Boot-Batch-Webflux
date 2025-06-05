package com.keakimleang.springbatchwebflux.repos;

import com.keakimleang.springbatchwebflux.entities.*;
import org.springframework.data.r2dbc.repository.*;
import reactor.core.publisher.*;

public interface BatchUploadProdRepository extends R2dbcRepository<BatchUploadProd, Long> {

    @Query("DELETE FROM batches_uploads_prod WHERE batch_upload_id = :batchUploadId")
    Mono<Long> deleteByBatchUploadId(Long batchUploadId);
}
