package com.keakimleang.springbatchwebflux.payloads;

import java.util.*;
import lombok.*;

@Getter
public class ApiValidationException extends BatchServiceException {
    private final List<ApiError> errors;

    public ApiValidationException(final List<ApiError> errors) {
        this.errors = errors;
    }

    public ApiValidationException(final ApiError error) {
        this.errors = Collections.singletonList(error);
    }
}
