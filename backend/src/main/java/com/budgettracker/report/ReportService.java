package com.budgettracker.report;

import static com.budgettracker.domain.transaction.TransactionSpecifications.accountIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrAfter;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrBefore;

import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import com.budgettracker.domain.transaction.CategorySpendingView;
import com.budgettracker.domain.transaction.DailyIncomeExpenseView;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import com.budgettracker.domain.transaction.TransactionSummaryView;
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
    private final TagRepository tagRepository;

    public ReportService(
        TransactionRepository transactionRepository,
        TagRepository tagRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public SummaryReportResponse summary(ReportFilterRequest filter) {
        TransactionSummaryView summary = transactionRepository.summarizeTransactions(
            filter.dateFrom(),
            filter.dateTo(),
            filter.accountId(),
            TransactionDirection.INCOME,
            TransactionDirection.EXPENSE
        );
        BigDecimal totalIncome = zeroIfNull(summary.getTotalIncome());
        BigDecimal totalExpenses = zeroIfNull(summary.getTotalExpenses());
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        return new SummaryReportResponse(
            money(totalIncome),
            money(totalExpenses),
            money(netSavings),
            percentage(netSavings, totalIncome),
            summary.getTransactionCount()
        );
    }

    @Transactional(readOnly = true)
    public List<SpendingByCategoryResponse> spendingByCategory(ReportFilterRequest filter) {
        List<CategorySpendingView> spending = transactionRepository.summarizeSpendingByCategory(
            filter.dateFrom(),
            filter.dateTo(),
            filter.accountId(),
            TransactionDirection.EXPENSE
        );
        BigDecimal totalExpenses = spending.stream()
            .map(CategorySpendingView::getTotalAmount)
            .map(this::zeroIfNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return spending.stream()
            .map(row -> spendingResponse(row, totalExpenses))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IncomeVsExpensesResponse> incomeVsExpenses(ReportFilterRequest filter) {
        return transactionRepository.summarizeIncomeVsExpensesByDate(
                filter.dateFrom(),
                filter.dateTo(),
                filter.accountId(),
                TransactionDirection.INCOME,
                TransactionDirection.EXPENSE
            )
            .stream()
            .collect(Collectors.groupingBy(
                row -> period(row.getTransactionDate(), filter.grouping()),
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .entrySet()
            .stream()
            .map(entry -> incomeVsExpensesResponse(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(IncomeVsExpensesResponse::period))
            .toList();
    }

    @Transactional(readOnly = true)
    public PropertyReportResponse property(ReportFilterRequest filter) {
        List<Transaction> transactions = findTransactions(filter);
        Map<Integer, Tag> tags = tagRepository.findAllById(
            transactions.stream()
                .map(Transaction::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(Tag::getId, Function.identity()));

        BigDecimal rentalIncome = propertyTotal(transactions, tags, "rental income", TransactionDirection.INCOME);
        BigDecimal mortgage = propertyTotal(transactions, tags, "mortgage", TransactionDirection.EXPENSE);
        BigDecimal insurance = propertyTotal(transactions, tags, "insurance", TransactionDirection.EXPENSE);
        BigDecimal rates = propertyTotal(transactions, tags, "rates", TransactionDirection.EXPENSE);
        BigDecimal repairs = propertyTotal(transactions, tags, "repairs", TransactionDirection.EXPENSE);
        BigDecimal propertyManagementFees = propertyTotal(
            transactions,
            tags,
            "property management",
            TransactionDirection.EXPENSE
        );
        BigDecimal otherPropertyExpenses = propertyTotal(
            transactions,
            tags,
            "other property expense",
            TransactionDirection.EXPENSE
        );
        BigDecimal totalPropertyIncome = rentalIncome;
        BigDecimal totalPropertyExpenses = mortgage
            .add(insurance)
            .add(rates)
            .add(repairs)
            .add(propertyManagementFees)
            .add(otherPropertyExpenses);

        return new PropertyReportResponse(
            money(rentalIncome),
            money(mortgage),
            money(insurance),
            money(rates),
            money(repairs),
            money(propertyManagementFees),
            money(otherPropertyExpenses),
            money(totalPropertyIncome),
            money(totalPropertyExpenses),
            money(totalPropertyIncome.subtract(totalPropertyExpenses))
        );
    }

    private List<Transaction> findTransactions(ReportFilterRequest filter) {
        Specification<Transaction> specification = Specification
            .where(transactionDateOnOrAfter(filter.dateFrom()))
            .and(transactionDateOnOrBefore(filter.dateTo()))
            .and(accountIdEquals(filter.accountId()));

        return transactionRepository.findAll(specification);
    }

    private SpendingByCategoryResponse spendingResponse(CategorySpendingView spending, BigDecimal totalExpenses) {
        BigDecimal totalAmount = zeroIfNull(spending.getTotalAmount());
        String categoryName = Optional.ofNullable(spending.getCategoryName()).orElse(UNCATEGORISED);

        return new SpendingByCategoryResponse(
            spending.getCategoryId(),
            categoryName,
            money(totalAmount),
            percentage(totalAmount, totalExpenses)
        );
    }

    private IncomeVsExpensesResponse incomeVsExpensesResponse(String period, List<DailyIncomeExpenseView> rows) {
        BigDecimal totalIncome = rows.stream()
            .map(DailyIncomeExpenseView::getTotalIncome)
            .map(this::zeroIfNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = rows.stream()
            .map(DailyIncomeExpenseView::getTotalExpenses)
            .map(this::zeroIfNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private BigDecimal propertyTotal(
        List<Transaction> transactions,
        Map<Integer, Tag> tags,
        String tagName,
        TransactionDirection direction
    ) {
        return transactions.stream()
            .filter(transaction -> transaction.getDirection() == direction)
            .filter(transaction -> hasTagName(transaction, tags, tagName))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasTagName(Transaction transaction, Map<Integer, Tag> tags, String tagName) {
        return Optional.ofNullable(tags.get(transaction.getTagId()))
            .map(Tag::getName)
            .map(name -> name.trim().toLowerCase())
            .filter(tagName::equals)
            .isPresent();
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

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
