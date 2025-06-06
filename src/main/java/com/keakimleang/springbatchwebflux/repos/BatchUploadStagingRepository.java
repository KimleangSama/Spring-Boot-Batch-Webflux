package com.keakimleang.springbatchwebflux.repos;

import com.keakimleang.springbatchwebflux.entities.*;
import java.util.*;
import org.springframework.data.r2dbc.repository.*;
import reactor.core.publisher.*;

public interface BatchUploadStagingRepository extends R2dbcRepository<BatchUploadStaging, Long> {

    @Query("DELETE FROM batches_uploads_staging WHERE batch_upload_id = :batchUploadId")
    Mono<Long> deleteByBatchUploadId(Long batchUploadId);

    List<BatchUploadStaging> findByBatchUploadId(Long batchUploadId);
}
