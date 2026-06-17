package com.budgettracker.report;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ReportFilterRequest(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate dateFrom,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate dateTo,

    Integer accountId,

    ReportGroupBy groupBy
) {

    public ReportGroupBy grouping() {
        return groupBy == null ? ReportGroupBy.MONTH : groupBy;
    }
}
