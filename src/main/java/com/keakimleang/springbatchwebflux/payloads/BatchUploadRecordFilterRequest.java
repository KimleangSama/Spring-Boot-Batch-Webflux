package com.keakimleang.springbatchwebflux.payloads;

import lombok.*;

@Getter
@Setter
@ToString
public class BatchUploadRecordFilterRequest {
    private Long batchUploadId;
    private String batchOwnerName;
}
