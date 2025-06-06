package com.keakimleang.springbatchwebflux.services;

import co.elastic.clients.elasticsearch.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import com.keakimleang.springbatchwebflux.batches.consts.*;
import com.keakimleang.springbatchwebflux.entities.*;
import com.keakimleang.springbatchwebflux.repos.*;
import java.util.function.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;
import reactor.core.scheduler.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final BatchUploadProdRepository batchUploadProdRepository;
    private final ElasticsearchClient elasticsearchClient;

    public Mono<SearchResponse<BatchUploadProdElasticsearch>> fuzzySearchCustomerCode(String approximateCustomerCode) {
        Supplier<Query> supplier = createSupplierQuery(approximateCustomerCode);

        return Mono.fromCallable(() -> {
                    SearchResponse<BatchUploadProdElasticsearch> response = elasticsearchClient
                            .search(s -> s.index(BatchFieldName.BATCHES_UPLOADS_PROD).query(supplier.get())
                                    .size(25), BatchUploadProdElasticsearch.class);
                    log.info("Elasticsearch supplier fuzzy query {}", supplier.get());
                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic()); // Run blocking I/O on a separate thread
    }

    private static FuzzyQuery createFuzzyQuery(String approximateCustomerCode) {
        return new FuzzyQuery.Builder()
                .field("customerCode")
                .value(approximateCustomerCode)
                .build();
    }

    private static Supplier<Query> createSupplierQuery(String approximateCustomerCode) {
        return () -> Query.of(q -> q.fuzzy(createFuzzyQuery(approximateCustomerCode)));
    }

    public void migrateDataToElasticsearch() {
        batchUploadProdRepository.findAll()
                .flatMap(batchUploadProd -> Mono.fromCallable(() -> {
                    final var batchUploadProdElasticsearch = new BatchUploadProdElasticsearch(batchUploadProd);
                    IndexRequest<BatchUploadProdElasticsearch> indexRequest = IndexRequest.of(i -> i
                            .index(BatchFieldName.BATCHES_UPLOADS_PROD)
                            .id(batchUploadProdElasticsearch.getId().toString())
                            .document(batchUploadProdElasticsearch));
                    elasticsearchClient.index(indexRequest);
                    log.info("Indexed batch upload prod with ID: {}", batchUploadProdElasticsearch.getId());
                    return batchUploadProdElasticsearch;
                }))
                .subscribeOn(Schedulers.boundedElastic()) // Run on background thread
                .doOnError(error -> log.error("Error during migration to Elasticsearch", error))
                .doOnComplete(() -> log.info("Migration to Elasticsearch completed successfully"))
                .subscribe(); // Trigger the execution
    }
}
