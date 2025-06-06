package com.keakimleang.springbatchwebflux.repos;

import com.keakimleang.springbatchwebflux.entities.*;
import org.springframework.data.elasticsearch.repository.config.*;
import org.springframework.data.keyvalue.repository.*;
import org.springframework.data.r2dbc.repository.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

public interface BatchUploadProdRepository extends R2dbcRepository<BatchUploadProd, Long> {

    @Modifying
    @Query("""
                    INSERT INTO batches_uploads_prod (
                        batch_upload_id,
                        customer_code,
                        invoice_date,
                        due_amount,
                        currency,
                        remark,
                        is_valid,
                        created_at
                    )
                    SELECT batch_upload_id, customer_code, invoice_date, due_amount,
                        currency, remark, true, created_at
                    FROM batches_uploads_staging staging
                    WHERE staging.batch_upload_id = :batchUploadId AND staging.is_valid = true
            """)
    Mono<Long> moveValidRecordFromStagingToProd(Long batchUploadId);

    Flux<BatchUploadProd> findByBatchUploadId(Long batchUploadId);
}
