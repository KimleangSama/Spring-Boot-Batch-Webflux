package com.keakimleang.springbatchwebflux.batches;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.extensions.excel.*;
import org.springframework.batch.extensions.excel.support.rowset.*;
import org.springframework.stereotype.*;

@Component
@StepScope
public class ExcelBatchUploadMapper implements RowMapper<BatchUploadItem> {
    // We want to robust handling if header contains leading space or trailing space
    private Map<String, String> headerMaps;

    @Override
    public BatchUploadItem mapRow(final RowSet rowSet) {
        if (Objects.isNull(headerMaps)) {
            headerMaps = rowSet.getProperties().stringPropertyNames()
                    .stream()
                    .collect(Collectors.toMap(
                            // We already validate header of file, and it is better convert it to lower case for
                            // consistent without worry about Amount or amount or AMount
                            km -> km.strip().toLowerCase(),
                            kv -> kv
                    ));
        }
        final var props = rowSet.getProperties();
        final var item = new BatchUploadItem();
        item.setCustomerCode(getStringValue(BatchFieldName.CUSTOMER_CODE, props));
        item.setInvoiceDate(getStringValue(BatchFieldName.INVOICE_DATE, props));
        item.setCurrency(getStringValue(BatchFieldName.CURRENCY, props));
        item.setDueAmount(getStringValue(BatchFieldName.DUE_AMOUNT, props));
        return item;
    }

    private String getStringValue(final String headerName,
                                  final Properties props) {
        final var rawHeader = headerMaps.get(headerName);
        return Objects.nonNull(rawHeader) ? props.getProperty(rawHeader) : null;
    }
}
