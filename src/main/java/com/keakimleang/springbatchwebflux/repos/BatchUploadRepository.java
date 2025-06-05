package com.keakimleang.springbatchwebflux.repos;

import com.keakimleang.springbatchwebflux.entities.*;
import org.springframework.data.r2dbc.repository.*;
import reactor.core.publisher.*;

public interface BatchUploadRepository extends R2dbcRepository<BatchUpload, Long> {

    Mono<BatchUpload> findByIdAndStatus(Long id,
                                        String status);
}
