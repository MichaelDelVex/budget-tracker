package com.budgettracker.domain.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.account.AccountType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportBatchRepositoryTest {

    @Autowired
    private ImportBatchRepository importBatchRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void savesAndFindsImportBatchByAccountId() {
        Account account = accountRepository.save(
            new Account("Everyday " + UUID.randomUUID(), "Example Bank", AccountType.CHECKING)
        );
        String filename = "statement-" + UUID.randomUUID() + ".csv";
        importBatchRepository.save(new ImportBatch(account.getId(), filename, 12, 10, 0, 2, Instant.now()));

        assertThat(importBatchRepository.findByAccountId(account.getId()))
            .extracting(ImportBatch::getOriginalFilename)
            .contains(filename);
    }
}
