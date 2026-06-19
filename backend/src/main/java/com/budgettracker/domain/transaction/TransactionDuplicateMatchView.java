package com.budgettracker.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionDuplicateMatchView {

    Integer getId();

    LocalDate getTransactionDate();

    String getDescription();

    String getRawDescription();

    BigDecimal getAmount();

    TransactionDirection getDirection();

    Integer getCategoryId();

    Integer getTagId();
}
