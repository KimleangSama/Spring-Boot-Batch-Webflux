package com.keakimleang.springbatchwebflux.entities;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import java.time.*;
import lombok.*;
import lombok.experimental.*;
import org.springframework.data.annotation.*;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.relational.core.mapping.*;

@Table(BatchFieldName.BATCHES_UPLOADS)
@Getter
@Setter
@Accessors(chain = true)
public class BatchUpload {
    @Id
    private Long id;

    @Column("batch_owner_name")
    private String batchOwnerName;

    @Column("uploaded_at")
    private LocalDateTime uploadedAt;

    @Column("total_records")
    private int totalRecords;

    @Column("valid_records")
    private int validRecords;

    @Column("invalid_records")
    private int invalidRecords;

    @Column("status")
    private String status;

    @Column("finish_at")
    private LocalDateTime finishAt;

    @Column("submitted_at")
    private LocalDateTime submittedAt;
}
