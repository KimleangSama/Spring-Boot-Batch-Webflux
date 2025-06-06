package com.keakimleang.springbatchwebflux.entities;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import java.io.*;
import java.math.*;
import java.time.*;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.relational.core.mapping.*;

@Document(indexName = BatchFieldName.BATCHES_UPLOADS_PROD)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadProdElasticsearch implements Serializable {
    @Id
    private Long id;

    private Long batchUploadId;

    private String customerCode;

    private String invoiceDate;

    private BigDecimal dueAmount;

    private String currency;

    private String createdAt;

    public BatchUploadProdElasticsearch(BatchUploadProd prod) {
        this.id = prod.getId();
        this.batchUploadId = prod.getBatchUploadId();
        this.customerCode = prod.getCustomerCode();
        this.invoiceDate = prod.getInvoiceDate() != null ? prod.getInvoiceDate().toString() : null;
        this.dueAmount = prod.getDueAmount();
        this.currency = prod.getCurrency();
        this.createdAt = prod.getCreatedAt() != null ? prod.getCreatedAt().toString() : null;
    }
}
