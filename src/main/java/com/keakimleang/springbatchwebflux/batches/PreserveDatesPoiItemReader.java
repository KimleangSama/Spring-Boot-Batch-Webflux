package com.keakimleang.springbatchwebflux.batches;

import java.io.*;
import java.math.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.extensions.excel.poi.*;
import org.springframework.batch.item.*;
import org.springframework.core.io.*;
import org.springframework.lang.*;

public class PreserveDatesPoiItemReader<T> extends PoiItemReader<T> {

    // Constants for date column names (lowercase for case-insensitive comparison)
    private static final String INVOICE_DATE_COLUMN_NAME = "invoice date";
    private static final Set<String> DATE_COLUMN_NAMES = new HashSet<>();

    static {
        DATE_COLUMN_NAMES.add(INVOICE_DATE_COLUMN_NAME);
    }

    // Resource and tracking fields
    private Resource excelResource;
    private int currentRowNumber = 0; // Track the current row number

    // Caches and mappings
    private final Map<Integer, Map<Integer, String>> originalNumericValueCache = new HashMap<>();
    private final Map<String, Integer> dateColumnIndexMap = new HashMap<>();

    @Override
    public void setResource(final Resource resource) {
        super.setResource(resource);
        this.excelResource = resource;
    }

    @Override
    public void open(@NonNull final ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
        this.currentRowNumber = 0; // Reset row counter

        try {
            final Workbook workbook = WorkbookFactory.create(excelResource.getInputStream());
            final Sheet sheet = workbook.getSheetAt(0);

            // Pre-process the Excel file to preserve original numeric values
            preprocessExcelFile(sheet);

            workbook.close();
        } catch (final IOException e) {
            throw new ItemStreamException("Failed to pre-process Excel file", e);
        }
    }

    private void preprocessExcelFile(final Sheet sheet) {
        // First, identify which columns contain date values
        identifyDateColumns(sheet);

        // Then, extract original numeric values for date cells in each row
        extractOriginalNumericValues(sheet);
    }

    private void identifyDateColumns(final Sheet sheet) {
        final Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                final Cell cell = headerRow.getCell(i);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    final String headerName = cell.getStringCellValue().toLowerCase().trim();
                    if (isDateColumn(headerName)) {
                        dateColumnIndexMap.put(headerName, i);
                    }
                }
            }
        }
    }

    private boolean isDateColumn(final String columnName) {
        return DATE_COLUMN_NAMES.contains(columnName);
    }

    private void extractOriginalNumericValues(final Sheet sheet) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            final Row row = sheet.getRow(rowIndex);
            if (row != null) {
                extractNumericValuesFromRow(row, rowIndex);
            }
        }
    }

    private void extractNumericValuesFromRow(final Row row, final int rowIndex) {
        for (final Map.Entry<String, Integer> entry : dateColumnIndexMap.entrySet()) {
            final int columnIndex = entry.getValue();
            final Cell cell = row.getCell(columnIndex);

            if (isExcelDateCell(cell)) {
                final String originalValue = extractOriginalNumericValue(cell);
                storeOriginalValue(rowIndex, columnIndex, originalValue);
            }
        }
    }

    private boolean isExcelDateCell(final Cell cell) {
        return cell != null &&
                cell.getCellType() == CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(cell);
    }

    private String extractOriginalNumericValue(final Cell cell) {
        final double numericValue = cell.getNumericCellValue();
        String originalValue = BigDecimal.valueOf(numericValue).toPlainString();

        // Remove decimal part if it's just .0
        if (originalValue.endsWith(".0")) {
            originalValue = originalValue.substring(0, originalValue.length() - 2);
        }

        return originalValue;
    }

    private void storeOriginalValue(final int rowIndex, final int columnIndex, final String originalValue) {
        final Map<Integer, String> rowCache = originalNumericValueCache.computeIfAbsent(rowIndex, k -> new HashMap<>());
        rowCache.put(columnIndex, originalValue);
    }

    @Override
    public T read() throws Exception {
        final T item = super.read();

        if (item != null) {
            // Increment the row counter for each item read
            // The first row is the header (skipped), so we start at 1
            currentRowNumber++;

            // If we have a BatchUploadItem, restore original numeric values for date fields
            if (item instanceof BatchUploadItem) {
                restoreOriginalValues((BatchUploadItem) item);
            }
        }

        return item;
    }

    private void restoreOriginalValues(final BatchUploadItem batchItem) {
        final Map<Integer, String> rowCache = originalNumericValueCache.get(currentRowNumber);

        if (rowCache != null) {
            // Restore invoice date if we have the original value
            restoreOriginalValueIfAvailable(batchItem, rowCache);
        }
    }

    private void restoreOriginalValueIfAvailable(final BatchUploadItem batchItem, final Map<Integer, String> rowCache) {
        final Integer columnIndex = dateColumnIndexMap.get(PreserveDatesPoiItemReader.INVOICE_DATE_COLUMN_NAME);
        if (columnIndex != null && rowCache.containsKey(columnIndex)) {
            final String originalValue = rowCache.get(columnIndex);
            batchItem.setInvoiceDate(originalValue);
        }
    }
}
