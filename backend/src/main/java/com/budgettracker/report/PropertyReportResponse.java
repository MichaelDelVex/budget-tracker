package com.budgettracker.report;

import java.math.BigDecimal;

public record PropertyReportResponse(
    BigDecimal rentalIncome,
    BigDecimal mortgage,
    BigDecimal insurance,
    BigDecimal rates,
    BigDecimal repairs,
    BigDecimal propertyManagementFees,
    BigDecimal otherPropertyExpenses,
    BigDecimal totalPropertyIncome,
    BigDecimal totalPropertyExpenses,
    BigDecimal netPropertyPosition
) {
}
