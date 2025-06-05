package com.keakimleang.springbatchwebflux.batches;

import static com.keakimleang.springbatchwebflux.batches.consts.BatchFieldName.*;
import com.keakimleang.springbatchwebflux.payloads.*;
import com.keakimleang.springbatchwebflux.utils.*;
import jakarta.validation.*;
import java.io.*;
import java.net.*;
import java.util.*;
import lombok.extern.slf4j.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.buffer.*;
import org.springframework.http.codec.multipart.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

@Slf4j
@Component
public class BatchUploadValidator {
    private final Set<String> supportedFiles = Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv");
    private final Validator validator;

    public BatchUploadValidator(final Validator validator) {
        this.validator = validator;
    }

    public Mono<BatchUploadRequest> validateFile(final BatchUploadRequest request) {
        return Mono.just(request)
                .flatMap(r -> {
                    if (!supportedFiles.contains(getMedialType(r.file()))) {
                        final var msg = "Wrong Format! The file should be in .csv or .xlsx format.";
                        return Mono.error(new ApiValidationException(ApiError.validateInput("file", msg)));
                    }

                    final var fileName = r.file().filename();
                    if (fileName.endsWith(".csv")) {
                        return validateCsvHeader(r);
                    } else { // excel
                        return validateExcelHeader(r);
                    }
                });
    }

    private Mono<BatchUploadRequest> validateCsvHeader(final BatchUploadRequest request) {
        return DataBufferUtils.join(request.file().content())
                .flatMap(dataBuffer -> {
                    final var bufferedReader = new BufferedReader(new InputStreamReader(dataBuffer.asInputStream()));
                    final var headers = CsvHelper.readRow(bufferedReader, 0);
                    return validate(request, headers);
                });
    }

    private Mono<BatchUploadRequest> validateExcelHeader(final BatchUploadRequest request) {
        return DataBufferUtils.join(request.file().content())
                .flatMap(dataBuffer -> {
                    try {
                        final var wb = WorkbookFactory.create(dataBuffer.asInputStream());
                        final var headers = ExcelHelper.readRow(wb.getSheetAt(0), 0);
                        return validate(request, headers);
                    } catch (final IOException e) {
                        return Mono.error(new BatchServiceException("Failed to read file"));
                    }
                });
    }

    private Mono<? extends BatchUploadRequest> validate(BatchUploadRequest request, List<String> headers) {
        final String msg = "Invalid file template for batcher upload";
        if (headers.size() != TEMPLATE_HEADER.size()) {
            return Mono.error(new ApiValidationException(ApiError.validateInput("file", msg)));
        }
        for (var idx = 0; idx < TEMPLATE_HEADER.size(); idx++) {
            if (!StringWrapperUtils.equalsIgnoreCase(TEMPLATE_HEADER.get(idx), headers.get(idx).strip())) {
                return Mono.error(new ApiValidationException(ApiError.validateInput("file", msg)));
            }
        }
        return Mono.just(request);
    }

    private String getMedialType(final FilePart filePart) {
        return URLConnection.guessContentTypeFromName(filePart.filename());
    }

    public String validateItem(final BatchUploadItem billUploadItem) {
        final var errors = validator.validate(billUploadItem);
        return String.join("\n", errors.stream()
                .map(ConstraintViolation::getMessage)
                .toList());
    }

}
