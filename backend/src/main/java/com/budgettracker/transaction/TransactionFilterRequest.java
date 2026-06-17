package com.budgettracker.transaction;

import com.budgettracker.domain.transaction.TransactionDirection;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record TransactionFilterRequest(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
    Integer accountId,
    Integer categoryId,
    Integer tagId,
    TransactionDirection direction,
    String search
) {
}
