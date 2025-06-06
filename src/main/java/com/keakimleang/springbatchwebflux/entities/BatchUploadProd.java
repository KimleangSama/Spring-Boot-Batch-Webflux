package com.keakimleang.springbatchwebflux.entities;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import java.io.*;
import java.math.*;
import java.time.*;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.relational.core.mapping.*;

@Table(BatchFieldName.BATCHES_UPLOADS_PROD)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadProd implements Serializable {
    @Id
    private Long id;

    @Column("batch_upload_id")
    private Long batchUploadId;

    @Column("customer_code")
    private String customerCode;

    @Column("invoice_date")
    private LocalDate invoiceDate;

    @Column("due_amount")
    private BigDecimal dueAmount;

    @Column("currency")
    private String currency;

    @Column("created_at")
    private LocalDateTime createdAt;
}
