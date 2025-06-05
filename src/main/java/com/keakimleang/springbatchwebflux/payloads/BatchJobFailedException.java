package com.keakimleang.springbatchwebflux.payloads;

import lombok.*;

@Getter
public class BatchJobFailedException extends BatchServiceException {
    private final ErrorCode errorCode = ErrorCode.BATCH_JOB_ERROR;
    private final Long batchUploadId;

    public BatchJobFailedException(final Long batchUploadId,
                                   final String message) {
        super(message);
        this.batchUploadId = batchUploadId;
    }

    public BatchJobFailedException(final Long batchUploadId,
                                   final String message,
                                   final Throwable cause) {
        super(message, cause);
        this.batchUploadId = batchUploadId;
    }
}
