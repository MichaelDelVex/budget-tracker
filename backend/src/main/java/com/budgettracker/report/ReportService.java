package com.budgettracker.report;

import static com.budgettracker.domain.transaction.TransactionSpecifications.accountIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrAfter;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrBefore;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final String UNCATEGORISED = "Uncategorised";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public ReportService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public SummaryReportResponse summary(ReportFilterRequest filter) {
        List<Transaction> transactions = findTransactions(filter);
        BigDecimal totalIncome = total(transactions, TransactionDirection.INCOME);
        BigDecimal totalExpenses = total(transactions, TransactionDirection.EXPENSE);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        return new SummaryReportResponse(
            money(totalIncome),
            money(totalExpenses),
            money(netSavings),
            percentage(netSavings, totalIncome),
            transactions.size()
        );
    }

    @Transactional(readOnly = true)
    public List<SpendingByCategoryResponse> spendingByCategory(ReportFilterRequest filter) {
        List<Transaction> expenses = findTransactions(filter).stream()
            .filter(transaction -> transaction.getDirection() == TransactionDirection.EXPENSE)
            .toList();
        BigDecimal totalExpenses = expenses.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Integer, Category> categories = categoryRepository.findAllById(
            expenses.stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<Integer, List<Transaction>> byCategory = new LinkedHashMap<>();
        for (Transaction transaction : expenses) {
            byCategory.computeIfAbsent(transaction.getCategoryId(), ignored -> new java.util.ArrayList<>())
                .add(transaction);
        }

        return byCategory
            .entrySet()
            .stream()
            .map(entry -> spendingResponse(entry.getKey(), entry.getValue(), categories, totalExpenses))
            .sorted(Comparator.comparing(SpendingByCategoryResponse::totalAmount).reversed())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IncomeVsExpensesResponse> incomeVsExpenses(ReportFilterRequest filter) {
        return findTransactions(filter).stream()
            .collect(Collectors.groupingBy(
                transaction -> period(transaction.getTransactionDate(), filter.grouping()),
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .entrySet()
            .stream()
            .map(entry -> incomeVsExpensesResponse(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(IncomeVsExpensesResponse::period))
            .toList();
    }

    private List<Transaction> findTransactions(ReportFilterRequest filter) {
        Specification<Transaction> specification = Specification
            .where(transactionDateOnOrAfter(filter.dateFrom()))
            .and(transactionDateOnOrBefore(filter.dateTo()))
            .and(accountIdEquals(filter.accountId()));

        return transactionRepository.findAll(specification);
    }

    private SpendingByCategoryResponse spendingResponse(
        Integer categoryId,
        List<Transaction> transactions,
        Map<Integer, Category> categories,
        BigDecimal totalExpenses
    ) {
        BigDecimal totalAmount = transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String categoryName = categoryId == null
            ? UNCATEGORISED
            : Optional.ofNullable(categories.get(categoryId)).map(Category::getName).orElse(UNCATEGORISED);

        return new SpendingByCategoryResponse(
            categoryId,
            categoryName,
            money(totalAmount),
            percentage(totalAmount, totalExpenses)
        );
    }

    private IncomeVsExpensesResponse incomeVsExpensesResponse(String period, List<Transaction> transactions) {
        BigDecimal totalIncome = total(transactions, TransactionDirection.INCOME);
        BigDecimal totalExpenses = total(transactions, TransactionDirection.EXPENSE);
        return new IncomeVsExpensesResponse(
            period,
            money(totalIncome),
            money(totalExpenses),
            money(totalIncome.subtract(totalExpenses))
        );
    }

    private BigDecimal total(List<Transaction> transactions, TransactionDirection direction) {
        return transactions.stream()
            .filter(transaction -> transaction.getDirection() == direction)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String period(LocalDate date, ReportGroupBy groupBy) {
        return switch (groupBy) {
            case FORTNIGHT -> YearMonth.from(date) + (date.getDayOfMonth() <= 14 ? " F1" : " F2");
            case MONTH -> YearMonth.from(date).toString();
            case QUARTER -> date.getYear() + " Q" + (((date.getMonthValue() - 1) / 3) + 1);
            case YEAR -> String.valueOf(date.getYear());
        };
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return value.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
