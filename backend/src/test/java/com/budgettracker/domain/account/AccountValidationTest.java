package com.budgettracker.domain.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class AccountValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameBankAndAccountType() {
        Account account = new Account(" ", "", null);

        assertThat(invalidProperties(account)).contains("name", "bank", "accountType");
    }
}
