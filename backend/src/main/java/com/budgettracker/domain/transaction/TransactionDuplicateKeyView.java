package com.budgettracker.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionDuplicateKeyView {

    LocalDate getTransactionDate();

    String getDescription();

    BigDecimal getAmount();
}
