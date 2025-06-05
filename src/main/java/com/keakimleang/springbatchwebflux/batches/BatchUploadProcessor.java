package com.keakimleang.springbatchwebflux.batches;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import com.keakimleang.springbatchwebflux.utils.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.jooq.*;
import org.jooq.impl.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.item.*;
import org.springframework.lang.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.*;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class BatchUploadProcessor implements ItemProcessor<BatchUploadItem, Map<String, Object>>, StepExecutionListener {

    private final BatchUploadValidator validator;
    private final DSLContext create;

    private Long uploadBatchId;

    private int invalidRecords;
    private int validRecords;


    @Override
    public Map<String, Object> process(@NonNull final BatchUploadItem item) {
        final var errorMsg = validator.validateItem(item);
        final boolean isValid;
        if (StringWrapperUtils.isNotBlank(errorMsg)) {
            invalidRecords = invalidRecords + 1;
            isValid = false;
        } else {
            validRecords = validRecords + 1;
            isValid = true;
        }

        final var now = DateUtil.now();
        final var map = new LinkedHashMap<String, Object>();
        map.put("batch_upload_id", uploadBatchId);
        map.put("customer_code", item.getCustomerCode());
        map.put("invoice_date", parseDate(item.getInvoiceDate()));
        map.put("due_amount", parseAmount(item.getDueAmount()));
        map.put("currency", item.getCurrency());
        map.put("remark", errorMsg);
        map.put("is_valid", isValid);
        map.put("created_at", now);
        return map;
    }

    private LocalDate parseDate(final String value) {
        return CastObjectUtil.getLocalDate(value, "yyyyMMdd");
    }

    private BigDecimal parseAmount(final String amount) {
        return CastObjectUtil.getBigDecimal(amount, 2, RoundingMode.DOWN, null);
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {
        final var jobParams = stepExecution.getJobExecution().getJobParameters();
        uploadBatchId = jobParams.getLong(BatchFieldName.UPLOAD_BATCH_ID);
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        final var finishAt = stepExecution.getEndTime();
        final var total = stepExecution.getWriteCount();
        final var data = new LinkedHashMap<String, Object>();
        data.put("total_records", total);
        data.put("valid_records", validRecords);
        data.put("invalid_records", invalidRecords);
        data.put("status", "AWAIT_CONFIRM");
        data.put("finish_at", finishAt);
        create.update(DSL.table(BatchFieldName.BATCHES_UPLOADS))
                .set(data)
                .where(DSL.field("id").eq(uploadBatchId))
                .execute();
        return ExitStatus.COMPLETED;
    }

}
