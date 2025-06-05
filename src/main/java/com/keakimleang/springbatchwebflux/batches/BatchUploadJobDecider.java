package com.keakimleang.springbatchwebflux.batches;

import static com.keakimleang.springbatchwebflux.batches.consts.BatchFieldName.*;
import com.keakimleang.springbatchwebflux.utils.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.flow.*;
import org.springframework.lang.*;
import org.springframework.stereotype.*;

@Component
public class BatchUploadJobDecider implements JobExecutionDecider {

    @NonNull
    @Override
    public FlowExecutionStatus decide(final JobExecution jobExecution,
                                      final StepExecution stepExecution) {
        final var fileName = jobExecution.getJobParameters()
                .getString(UPLOAD_FILE);
        if (StringWrapperUtils.endsWithIgnoreCase(fileName, ".xlsx")) {
            return new FlowExecutionStatus("START_UPLOAD_EXCEL_STEP");
        }
        if (StringWrapperUtils.endsWithIgnoreCase(fileName, ".csv")) {
            return new FlowExecutionStatus("START_UPLOAD_CSV_STEP");
        }
        return FlowExecutionStatus.FAILED;
    }
}
