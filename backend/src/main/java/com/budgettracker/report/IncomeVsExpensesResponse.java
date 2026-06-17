package com.budgettracker.report;

import java.math.BigDecimal;

public record IncomeVsExpensesResponse(
    String period,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netSavings
) {
}
