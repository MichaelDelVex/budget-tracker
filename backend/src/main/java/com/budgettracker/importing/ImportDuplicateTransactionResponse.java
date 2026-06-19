package com.budgettracker.importing;

import com.budgettracker.domain.transaction.TransactionDirection;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportDuplicateTransactionResponse(
    Integer id,
    Integer rowNumber,
    LocalDate transactionDate,
    String description,
    String rawDescription,
    BigDecimal amount,
    TransactionDirection direction,
    Integer categoryId,
    Integer tagId
) {
}
