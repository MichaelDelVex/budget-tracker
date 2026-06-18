package com.budgettracker.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
    extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {

    boolean existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount
    );

    boolean existsByAccountIdAndTransactionDateAndDescriptionAndAmountAndIdNot(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        Integer id
    );

    @Query("""
        select
            transaction.transactionDate as transactionDate,
            transaction.description as description,
            transaction.amount as amount
        from Transaction transaction
        where transaction.accountId = :accountId
            and transaction.transactionDate between :dateFrom and :dateTo
        """)
    List<TransactionDuplicateKeyView> findDuplicateKeysForAccountAndDateRange(
        @Param("accountId") Integer accountId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo
    );
}
