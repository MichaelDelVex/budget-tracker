package com.budgettracker.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionDuplicateMatchView {

    Integer getId();

    LocalDate getTransactionDate();

    String getDescription();

    BigDecimal getAmount();

    TransactionDirection getDirection();
}
