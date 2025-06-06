package com.keakimleang.springbatchwebflux;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import com.keakimleang.springbatchwebflux.entities.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.*;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

@RequiredArgsConstructor
@Component
@Slf4j
public class ElasticSearchStartupMigration implements CommandLineRunner {
    private final ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    @Override
    public void run(String... args) {
        try {
            final var indexOps = reactiveElasticsearchOperations.indexOps(IndexCoordinates.of(BatchFieldName.BATCHES_UPLOADS_PROD));
            indexOps.exists()
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            log.info("Index already exists");
                            return Mono.empty();
                        } else {
                            log.info("Indexing uploaded files");
                            return indexOps.create().then(indexOps.putMapping(BatchUploadProd.class));
                        }
                    })
                    .subscribe();
        } catch (Exception exception) {
            log.error("Elastic Service: {}", exception.getMessage());
        }
    }
}
