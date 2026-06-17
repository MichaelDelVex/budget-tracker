package com.budgettracker.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.account.AccountType;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
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

    private Transaction transaction(Integer accountId, Integer categoryId, LocalDate date, String amount) {
        return new Transaction(
            accountId,
            date,
            "Filtered expense " + amount,
            "Filtered expense " + amount,
            new BigDecimal(amount),
            TransactionDirection.EXPENSE,
            categoryId,
            null,
            null
        );
    }
}
