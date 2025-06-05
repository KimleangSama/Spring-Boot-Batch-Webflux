package com.keakimleang.springbatchwebflux.batches;

import static com.keakimleang.springbatchwebflux.batches.consts.BatchFieldName.*;
import org.jooq.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.stereotype.*;

@Component
@StepScope
public class BatchUploadWriter extends JooqItemWriter {

    public BatchUploadWriter(final DSLContext create) {
        super(create, BATCHES_UPLOADS_STAGING);
    }
}
