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
        return new Transaction(
            accountId,
            date,
            "Filtered expense " + amount,
            "Filtered expense " + amount,
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
