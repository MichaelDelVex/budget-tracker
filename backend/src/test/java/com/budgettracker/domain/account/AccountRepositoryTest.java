package com.budgettracker.domain.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void savesAndFindsAccountByName() {
        String name = "Everyday " + UUID.randomUUID();
        accountRepository.save(new Account(name, "Example Bank", AccountType.CHECKING));

        assertThat(accountRepository.findByName(name))
            .isPresent()
            .get()
            .extracting(Account::getAccountType)
            .isEqualTo(AccountType.CHECKING);
    }
}
