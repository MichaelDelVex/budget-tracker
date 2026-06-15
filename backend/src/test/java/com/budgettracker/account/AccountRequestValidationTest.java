package com.budgettracker.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class AccountRequestValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameBankAndAccountType() {
        AccountRequest request = new AccountRequest("", " ", null);

        assertThat(invalidProperties(request)).contains("name", "bank", "accountType");
    }

    @Test
    void enforcesLengthLimits() {
        String tooLong = "x".repeat(121);
        AccountRequest request = new AccountRequest(tooLong, tooLong, null);

        assertThat(invalidProperties(request)).contains("name", "bank", "accountType");
    }
}
