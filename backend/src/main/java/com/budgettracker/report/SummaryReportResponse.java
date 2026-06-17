package com.budgettracker.report;

import java.math.BigDecimal;

public record SummaryReportResponse(
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netSavings,
    BigDecimal savingsPercentage,
    long transactionCount
) {
}
