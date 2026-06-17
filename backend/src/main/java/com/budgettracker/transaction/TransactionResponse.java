package com.budgettracker.transaction;

import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
    Integer id,
    Integer accountId,
    LocalDate transactionDate,
    String description,
    String rawDescription,
    BigDecimal amount,
    TransactionDirection direction,
    Integer categoryId,
    Integer tagId,
    Integer importBatchId,
    Instant createdAt,
    Instant updatedAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getAccountId(),
            transaction.getTransactionDate(),
            transaction.getDescription(),
            transaction.getRawDescription(),
            transaction.getAmount(),
            transaction.getDirection(),
            transaction.getCategoryId(),
            transaction.getTagId(),
            transaction.getImportBatchId(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }
}
