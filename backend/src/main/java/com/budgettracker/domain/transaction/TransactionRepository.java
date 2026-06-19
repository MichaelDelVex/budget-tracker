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

    void deleteByAccountId(Integer accountId);

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
            transaction.id as id,
            transaction.transactionDate as transactionDate,
            transaction.description as description,
            transaction.rawDescription as rawDescription,
            transaction.amount as amount,
            transaction.direction as direction,
            transaction.categoryId as categoryId,
            transaction.tagId as tagId
        from Transaction transaction
        where transaction.accountId = :accountId
            and transaction.transactionDate between :dateFrom and :dateTo
        """)
    List<TransactionDuplicateMatchView> findDuplicateMatchesForAccountAndDateRange(
        @Param("accountId") Integer accountId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo
    );

    @Query("""
        select
            coalesce(sum(case when transaction.direction = :income then transaction.amount else 0 end), 0) as totalIncome,
            coalesce(sum(case when transaction.direction = :expense then transaction.amount else 0 end), 0) as totalExpenses,
            count(transaction) as transactionCount
        from Transaction transaction
        where (:dateFrom is null or transaction.transactionDate >= :dateFrom)
            and (:dateTo is null or transaction.transactionDate <= :dateTo)
            and (:accountId is null or transaction.accountId = :accountId)
        """)
    TransactionSummaryView summarizeTransactions(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        @Param("accountId") Integer accountId,
        @Param("income") TransactionDirection income,
        @Param("expense") TransactionDirection expense
    );

    @Query("""
        select
            transaction.categoryId as categoryId,
            category.name as categoryName,
            sum(transaction.amount) as totalAmount
        from Transaction transaction
        left join Category category on category.id = transaction.categoryId
        where transaction.direction = :expense
            and (:dateFrom is null or transaction.transactionDate >= :dateFrom)
            and (:dateTo is null or transaction.transactionDate <= :dateTo)
            and (:accountId is null or transaction.accountId = :accountId)
        group by transaction.categoryId, category.name
        order by sum(transaction.amount) desc
        """)
    List<CategorySpendingView> summarizeSpendingByCategory(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        @Param("accountId") Integer accountId,
        @Param("expense") TransactionDirection expense
    );

    @Query("""
        select
            transaction.transactionDate as transactionDate,
            coalesce(sum(case when transaction.direction = :income then transaction.amount else 0 end), 0) as totalIncome,
            coalesce(sum(case when transaction.direction = :expense then transaction.amount else 0 end), 0) as totalExpenses
        from Transaction transaction
        where (:dateFrom is null or transaction.transactionDate >= :dateFrom)
            and (:dateTo is null or transaction.transactionDate <= :dateTo)
            and (:accountId is null or transaction.accountId = :accountId)
        group by transaction.transactionDate
        order by transaction.transactionDate
        """)
    List<DailyIncomeExpenseView> summarizeIncomeVsExpensesByDate(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        @Param("accountId") Integer accountId,
        @Param("income") TransactionDirection income,
        @Param("expense") TransactionDirection expense
    );
}
