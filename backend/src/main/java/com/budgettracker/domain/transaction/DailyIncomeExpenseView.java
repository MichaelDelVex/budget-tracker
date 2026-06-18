package com.budgettracker.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyIncomeExpenseView {

    LocalDate getTransactionDate();

    BigDecimal getTotalIncome();

    BigDecimal getTotalExpenses();
}
