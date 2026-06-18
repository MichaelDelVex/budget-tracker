package com.budgettracker.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.account.AccountType;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Test
    void dateAndAccountFiltersLimitReportResults() {
        Account account = accountRepository.save(new Account(
            "Report account " + UUID.randomUUID(),
            "Example Bank",
            AccountType.CHECKING
        ));
        Account otherAccount = accountRepository.save(new Account(
            "Other report account " + UUID.randomUUID(),
            "Example Bank",
            AccountType.CHECKING
        ));
        Category category = categoryRepository.findByName("Groceries").orElseThrow();

        transactionRepository.save(transaction(account.getId(), category.getId(), LocalDate.of(2026, 1, 10), "50.00"));
        transactionRepository.save(transaction(account.getId(), category.getId(), LocalDate.of(2026, 2, 10), "90.00"));
        transactionRepository.save(transaction(otherAccount.getId(), category.getId(), LocalDate.of(2026, 1, 10), "70.00"));

        SummaryReportResponse response = reportService.summary(new ReportFilterRequest(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            account.getId(),
            ReportGroupBy.MONTH
        ));

        assertThat(response.totalExpenses()).isEqualByComparingTo("50.00");
        assertThat(response.transactionCount()).isEqualTo(1);
    }

    @Test
    void databaseAggregationsMatchExpectedReportResults() {
        Account account = accountRepository.save(new Account(
            "Aggregation report account " + UUID.randomUUID(),
            "Example Bank",
            AccountType.CHECKING
        ));
        Account otherAccount = accountRepository.save(new Account(
            "Other aggregation report account " + UUID.randomUUID(),
            "Example Bank",
            AccountType.CHECKING
        ));
        Category groceries = categoryRepository.findByName("Groceries").orElseThrow();
        Category dining = categoryRepository.findByName("Dining").orElseThrow();

        transactionRepository.save(transaction(account.getId(), null, LocalDate.of(2026, 1, 5), "2000.00", TransactionDirection.INCOME, null, "Salary"));
        transactionRepository.save(transaction(account.getId(), groceries.getId(), LocalDate.of(2026, 1, 10), "100.00", TransactionDirection.EXPENSE, null, "Groceries"));
        transactionRepository.save(transaction(account.getId(), dining.getId(), LocalDate.of(2026, 1, 11), "50.00", TransactionDirection.EXPENSE, null, "Dining"));
        transactionRepository.save(transaction(account.getId(), groceries.getId(), LocalDate.of(2026, 2, 10), "25.00", TransactionDirection.EXPENSE, null, "February groceries"));
        transactionRepository.save(transaction(otherAccount.getId(), groceries.getId(), LocalDate.of(2026, 1, 10), "999.00", TransactionDirection.EXPENSE, null, "Other account"));

        ReportFilterRequest januaryAccountFilter = new ReportFilterRequest(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            account.getId(),
            ReportGroupBy.MONTH
        );

        SummaryReportResponse summary = reportService.summary(januaryAccountFilter);
        assertThat(summary.totalIncome()).isEqualByComparingTo("2000.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("150.00");
        assertThat(summary.netSavings()).isEqualByComparingTo("1850.00");
        assertThat(summary.savingsPercentage()).isEqualByComparingTo("92.50");
        assertThat(summary.transactionCount()).isEqualTo(3);

        List<SpendingByCategoryResponse> spending = reportService.spendingByCategory(januaryAccountFilter);
        assertThat(spending).hasSize(2);
        assertThat(spending.get(0).categoryName()).isEqualTo("Groceries");
        assertThat(spending.get(0).totalAmount()).isEqualByComparingTo("100.00");
        assertThat(spending.get(0).percentageOfExpenses()).isEqualByComparingTo("66.67");
        assertThat(spending.get(1).categoryName()).isEqualTo("Dining");
        assertThat(spending.get(1).totalAmount()).isEqualByComparingTo("50.00");
        assertThat(spending.get(1).percentageOfExpenses()).isEqualByComparingTo("33.33");

        List<IncomeVsExpensesResponse> incomeVsExpenses = reportService.incomeVsExpenses(new ReportFilterRequest(
            null,
            null,
            account.getId(),
            ReportGroupBy.MONTH
        ));
        assertThat(incomeVsExpenses)
            .extracting(IncomeVsExpensesResponse::period)
            .containsExactly("2026-01", "2026-02");
        assertThat(incomeVsExpenses.get(0).totalIncome()).isEqualByComparingTo("2000.00");
        assertThat(incomeVsExpenses.get(0).totalExpenses()).isEqualByComparingTo("150.00");
        assertThat(incomeVsExpenses.get(0).netSavings()).isEqualByComparingTo("1850.00");
        assertThat(incomeVsExpenses.get(1).totalIncome()).isEqualByComparingTo("0.00");
        assertThat(incomeVsExpenses.get(1).totalExpenses()).isEqualByComparingTo("25.00");
        assertThat(incomeVsExpenses.get(1).netSavings()).isEqualByComparingTo("-25.00");
    }

    @Test
    void emptyReportFiltersReturnSafeValues() {
        ReportFilterRequest emptyFilter = new ReportFilterRequest(
            LocalDate.of(2099, 1, 1),
            LocalDate.of(2099, 1, 31),
            null,
            ReportGroupBy.MONTH
        );

        SummaryReportResponse summary = reportService.summary(emptyFilter);
        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(summary.netSavings()).isEqualByComparingTo("0.00");
        assertThat(summary.savingsPercentage()).isEqualByComparingTo("0.00");
        assertThat(summary.transactionCount()).isZero();
        assertThat(reportService.spendingByCategory(emptyFilter)).isEmpty();
        assertThat(reportService.incomeVsExpenses(emptyFilter)).isEmpty();
    }

    @Test
    void dateAndAccountFiltersLimitPropertyReportResults() {
        Account account = accountRepository.save(new Account(
            "Property report account " + UUID.randomUUID(),
            "Example Bank",
            AccountType.CHECKING
        ));
        Account otherAccount = accountRepository.save(new Account(
            "Other property report account " + UUID.randomUUID(),
            "Example Bank",
            AccountType.CHECKING
        ));
        Tag rentalIncome = findOrCreateTag("Rental Income");
        Tag mortgage = findOrCreateTag("Mortgage");

        transactionRepository.save(transaction(account.getId(), null, LocalDate.of(2026, 1, 10), "2000.00", TransactionDirection.INCOME, rentalIncome.getId()));
        transactionRepository.save(transaction(account.getId(), null, LocalDate.of(2026, 2, 10), "999.00", TransactionDirection.INCOME, rentalIncome.getId()));
        transactionRepository.save(transaction(otherAccount.getId(), null, LocalDate.of(2026, 1, 10), "999.00", TransactionDirection.EXPENSE, mortgage.getId()));
        transactionRepository.save(transaction(account.getId(), null, LocalDate.of(2026, 1, 11), "1200.00", TransactionDirection.EXPENSE, mortgage.getId()));

        PropertyReportResponse response = reportService.property(new ReportFilterRequest(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            account.getId(),
            ReportGroupBy.MONTH
        ));

        assertThat(response.rentalIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.mortgage()).isEqualByComparingTo("1200.00");
        assertThat(response.totalPropertyIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.totalPropertyExpenses()).isEqualByComparingTo("1200.00");
        assertThat(response.netPropertyPosition()).isEqualByComparingTo("800.00");
    }

    private Transaction transaction(Integer accountId, Integer categoryId, LocalDate date, String amount) {
        return transaction(accountId, categoryId, date, amount, TransactionDirection.EXPENSE, null);
    }

    private Transaction transaction(
        Integer accountId,
        Integer categoryId,
        LocalDate date,
        String amount,
        TransactionDirection direction,
        Integer tagId
    ) {
        return transaction(accountId, categoryId, date, amount, direction, tagId, "Filtered expense " + amount);
    }

    private Transaction transaction(
        Integer accountId,
        Integer categoryId,
        LocalDate date,
        String amount,
        TransactionDirection direction,
        Integer tagId,
        String description
    ) {
        return new Transaction(
            accountId,
            date,
            description,
            description,
            new BigDecimal(amount),
            direction,
            categoryId,
            tagId,
            null
        );
    }

    private Tag findOrCreateTag(String name) {
        return tagRepository.findByName(name).orElseGet(() -> tagRepository.save(new Tag(name, "#336699")));
    }
}
