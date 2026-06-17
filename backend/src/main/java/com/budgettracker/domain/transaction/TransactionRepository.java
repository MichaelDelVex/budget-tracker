package com.budgettracker.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository
    extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {

    boolean existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount
    );
}
