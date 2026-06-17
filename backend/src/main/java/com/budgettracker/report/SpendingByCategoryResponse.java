package com.budgettracker.report;

import java.math.BigDecimal;

public record SpendingByCategoryResponse(
    Integer categoryId,
    String categoryName,
    BigDecimal totalAmount,
    BigDecimal percentageOfExpenses
) {
}
