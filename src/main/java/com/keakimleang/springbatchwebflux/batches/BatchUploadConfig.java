package com.keakimleang.springbatchwebflux.batches;

import com.keakimleang.springbatchwebflux.batches.consts.*;
import java.util.*;
import lombok.*;
import org.jooq.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.builder.*;
import org.springframework.batch.core.job.flow.*;
import org.springframework.batch.core.job.flow.support.*;
import org.springframework.batch.core.repository.*;
import org.springframework.batch.core.step.builder.*;
import org.springframework.batch.extensions.excel.poi.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.builder.*;
import org.springframework.batch.item.file.mapping.*;
import org.springframework.batch.item.file.transform.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.*;
import org.springframework.transaction.*;

@Configuration
@RequiredArgsConstructor
public class BatchUploadConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    @StepScope
    public FlatFileItemReader<BatchUploadItem> csvBatchUploadReader(@Value("file:#{jobParameters['uploadFile']}") final Resource resource) {
        final var lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");

        lineTokenizer.setNames(BatchFieldName.BATCH_UPLOAD_FIELD_NAMES);

        final var fieldSet = new BeanWrapperFieldSetMapper<BatchUploadItem>();
        fieldSet.setTargetType(BatchUploadItem.class);

        return new FlatFileItemReaderBuilder<BatchUploadItem>()
                .name("csvBatchUploadReader")
                .resource(resource)
                .linesToSkip(1)
                .lineTokenizer(lineTokenizer)
                .fieldSetMapper(fieldSet)
                .build();
    }

    @Bean
    @StepScope
    public PreserveDatesPoiItemReader<BatchUploadItem> excelBatchUploadReader(@Value("file:#{jobParameters['uploadFile']}") final Resource resource,
                                                                 final ExcelBatchUploadMapper excelBatchUploadMapper) {
        final var poiItemReader = new PreserveDatesPoiItemReader<BatchUploadItem>();
        poiItemReader.setName("excelBatchUploadReader");
        poiItemReader.setLinesToSkip(1);
        poiItemReader.setResource(resource);
        poiItemReader.setRowMapper(excelBatchUploadMapper);
        return poiItemReader;
    }

    @Bean
    @StepScope
    public BatchUploadWriter batchUploadWriter(final DSLContext create) {
        return new BatchUploadWriter(create, BatchFieldName.BATCHES_UPLOADS_STAGING);
    }

    @Bean
    public Step importExcelToDbStep(final PreserveDatesPoiItemReader<BatchUploadItem> excelBatchUploadReader,
                                    final BatchUploadProcessor batchUploadProcessor,
                                    final BatchUploadWriter batchUploadWriter) {
        return new StepBuilder("importExcelToDbStep", jobRepository)
                .<BatchUploadItem, Map<String, Object>>chunk(10, platformTransactionManager)
                .reader(excelBatchUploadReader)
                .processor(batchUploadProcessor)
                .writer(batchUploadWriter)
                .build();
    }

    @Bean
    public Step importCsvToDbStep(final FlatFileItemReader<BatchUploadItem> csvBatchUploadReader,
                                  final BatchUploadProcessor batchUploadProcessor,
                                  final BatchUploadWriter batchUploadWriter) {
        return new StepBuilder("importCsvToDbStep", jobRepository)
                .<BatchUploadItem, Map<String, Object>>chunk(10, platformTransactionManager)
                .reader(csvBatchUploadReader)
                .processor(batchUploadProcessor)
                .writer(batchUploadWriter)
                .build();
    }


    @Bean
    public Step cleanupResourceStep(final CleanupBatchUploadFileTasklet cleanupBatchUploadFileTasklet) {
        return new StepBuilder("cleanupResourceStep", jobRepository)
                .tasklet(cleanupBatchUploadFileTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    public Flow uploadExcelFlow(final Step importExcelToDbStep) {
        return new FlowBuilder<SimpleFlow>("uploadExcelFlow")
                .start(importExcelToDbStep)
                .on("COMPLETED")
                .end()
                .build();
    }

    @Bean
    public Flow uploadCsvFlow(final Step importCsvToDbStep) {
        return new FlowBuilder<SimpleFlow>("uploadCsvFlow")
                .start(importCsvToDbStep)
                .on("COMPLETED")
                .end()
                .build();
    }

    @Bean
    public Job uploadBatchJob(final BatchUploadJobDecider batchUploadJobDecider,
                              final Flow uploadExcelFlow,
                              final Flow uploadCsvFlow,
                              final Step cleanupResourceStep,
                              final JobExecutionListener batchUploadJobExecutionListener) {
        return new JobBuilder("uploadBatchJob", jobRepository)
                .listener(batchUploadJobExecutionListener)

                // Start with the decider to determine the next step
                .start(batchUploadJobDecider)

                // If the decider returns a specific value, execute the Excel import step
                .from(batchUploadJobDecider)
                .on("START_UPLOAD_EXCEL_STEP")
                .to(uploadExcelFlow)

                // If the decider returns a different value, execute the CSV import step
                .from(batchUploadJobDecider)
                .on("START_UPLOAD_CSV_STEP")
                .to(uploadCsvFlow)

                // Always cleanup resource and determine job success or failed based on previous step
                .from(uploadExcelFlow).on("*").to(cleanupResourceStep)
                .from(uploadCsvFlow).on("*").to(cleanupResourceStep)

                .end()
                .build();
    }
}
