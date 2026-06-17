package com.budgettracker.domain.transaction;

import static com.budgettracker.domain.transaction.TransactionSpecifications.accountIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.categoryIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.directionEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.searchDescriptions;
import static com.budgettracker.domain.transaction.TransactionSpecifications.tagIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrAfter;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrBefore;
import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.account.AccountType;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private Integer accountId;
    private Integer categoryId;
    private Integer tagId;

    @BeforeEach
    void setUp() {
        Account account = accountRepository.save(
            new Account("Everyday " + UUID.randomUUID(), "Example Bank", AccountType.CHECKING)
        );
        Category category = categoryRepository.findByName("Groceries").orElseThrow();
        Tag tag = tagRepository.save(new Tag("Shared " + UUID.randomUUID(), "#2F855A"));

        accountId = account.getId();
        categoryId = category.getId();
        tagId = tag.getId();
    }

    @Test
    void savesAndFindsTransaction() {
        Transaction transaction = transactionRepository.save(sampleTransaction("Groceries", TransactionDirection.EXPENSE));

        assertThat(transactionRepository.findById(transaction.getId()))
            .isPresent()
            .get()
            .extracting(Transaction::getDescription)
            .isEqualTo("Groceries");
    }

    @Test
    void filtersTransactionsByAllSupportedFilters() {
        transactionRepository.save(sampleTransaction("Weekly supermarket", TransactionDirection.EXPENSE));
        transactionRepository.save(new Transaction(
            accountId,
            LocalDate.of(2026, 2, 15),
            "Salary",
            "Payroll deposit",
            new BigDecimal("1000.00"),
            TransactionDirection.INCOME,
            null,
            null,
            null
        ));

        Specification<Transaction> specification = Specification
            .where(transactionDateOnOrAfter(LocalDate.of(2026, 1, 1)))
            .and(transactionDateOnOrBefore(LocalDate.of(2026, 1, 31)))
            .and(accountIdEquals(accountId))
            .and(categoryIdEquals(categoryId))
            .and(tagIdEquals(tagId))
            .and(directionEquals(TransactionDirection.EXPENSE))
            .and(searchDescriptions("supermarket"));

        assertThat(transactionRepository.findAll(specification))
            .extracting(Transaction::getDescription)
            .containsExactly("Weekly supermarket");
    }

    private Transaction sampleTransaction(String description, TransactionDirection direction) {
        return new Transaction(
            accountId,
            LocalDate.of(2026, 1, 10),
            description,
            "Raw " + description,
            new BigDecimal("42.50"),
            direction,
            categoryId,
            tagId,
            null
        );
    }
}
