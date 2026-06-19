package com.budgettracker.importing;

import com.budgettracker.domain.transaction.TransactionDirection;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UnmatchedImportCategoryRowResponse(
    int rowNumber,
    LocalDate transactionDate,
    String description,
    BigDecimal amount,
    TransactionDirection direction
) {
}
