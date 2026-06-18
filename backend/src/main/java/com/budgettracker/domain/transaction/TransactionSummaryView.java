package com.budgettracker.domain.transaction;

import java.math.BigDecimal;

public interface TransactionSummaryView {

    BigDecimal getTotalIncome();

    BigDecimal getTotalExpenses();

    long getTransactionCount();
}
