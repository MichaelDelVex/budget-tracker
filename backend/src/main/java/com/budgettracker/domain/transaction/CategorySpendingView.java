package com.budgettracker.domain.transaction;

import java.math.BigDecimal;

public interface CategorySpendingView {

    Integer getCategoryId();

    String getCategoryName();

    BigDecimal getTotalAmount();
}
