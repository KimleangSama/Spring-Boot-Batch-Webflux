package com.keakimleang.springbatchwebflux.batches;

import com.keakimleang.springbatchwebflux.annotations.*;
import static com.keakimleang.springbatchwebflux.utils.StringWrapperUtils.*;
import lombok.*;

@ToString
@Setter
@AtLeastOneField(fields = {"customerCode", "invoiceDate"}, message = "Provide at least Customer Code or Invoice Date")
public class BatchUploadItem {
    private String customerCode;
    @ValidDateFormat(pattern = "yyyyMMdd", message = "Invoice Date must be format yyyyMMdd", optional = true)
    private String invoiceDate;
    @ValidMonetary(message = "Due Amount must be positive number")
    private String dueAmount;
    @ValidCurrency(message = "Currency must be any value of [KHR, USD]")
    private String currency;

    public String getCustomerCode() {
        return strip(customerCode);
    }

    public String getInvoiceDate() {
        return strip(invoiceDate);
    }

    public String getDueAmount() {
        return strip(dueAmount);
    }

    public String getCurrency() {
        return upperCase(currency);
    }
}
