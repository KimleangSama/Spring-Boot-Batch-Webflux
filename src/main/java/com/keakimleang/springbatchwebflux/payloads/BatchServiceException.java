package com.keakimleang.springbatchwebflux.payloads;

public class BatchServiceException extends RuntimeException {

    public BatchServiceException() {
    }

    public BatchServiceException(final String message) {
        super(message);
    }

    public BatchServiceException(final String message,
                                 final Throwable cause) {
        super(message, cause);
    }
}
