package com.keakimleang.springbatchwebflux.repos;

import com.keakimleang.springbatchwebflux.entities.*;
import org.springframework.context.annotation.*;
import org.springframework.data.elasticsearch.repository.*;
import org.springframework.stereotype.*;

@Repository
@Lazy
public interface BatchUploadProdElasticsearchRepository extends ReactiveElasticsearchRepository<BatchUploadProdElasticsearch, Long> {

}
