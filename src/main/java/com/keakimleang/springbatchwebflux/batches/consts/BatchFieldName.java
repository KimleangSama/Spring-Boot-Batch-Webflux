package com.keakimleang.springbatchwebflux.batches.consts;

import java.util.*;

public class BatchFieldName {
    public static final String[] BATCH_UPLOAD_FIELD_NAMES = {
            "customerCode",
            "invoiceDate",
            "dueAmount",
            "currency",
    };

    public static final List<String> TEMPLATE_HEADER = List.of(
            "customer code",
            "invoice date",
            "due amount",
            "currency"
    );

    public static final String BATCHES_UPLOADS = "batches_uploads";
    public static final String BATCHES_UPLOADS_STAGING = "batches_uploads_staging";
    public static final String BATCHES_UPLOADS_PROD = "batches_uploads_prod";
    public static final String UPLOAD_FILE = "uploadFile";
    public static final String UPLOAD_BATCH_ID = "uploadBatchId";
    public static final String RUN_ASYNC_FLOW = "runAsyncFlow";

    public static final List<String> ORDERED_COLUMN = List.of(
            "id",
            "batchUploadId",
            "customerCode",
            "invoiceDate",
            "dueAmount",
            "currency"
    );

    public static final String CUSTOMER_CODE = "customer code";
    public static final String INVOICE_DATE = "invoice date";
    public static final String DUE_AMOUNT = "due amount";
    public static final String CURRENCY = "currency";
}
