package com.budgettracker.domain.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameAndType() {
        Account account = new Account(" ", null, BigDecimal.ZERO);

        assertThat(invalidProperties(account)).contains("name", "type");
    }
}
