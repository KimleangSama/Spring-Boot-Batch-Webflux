package com.keakimleang.springbatchwebflux.entities;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import java.math.*;
import java.time.*;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;

@Table(BatchFieldName.BATCHES_UPLOADS_STAGING)
@Getter
@Setter
public class BatchUploadStaging {
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
