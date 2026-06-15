package com.budgettracker.account;

import com.budgettracker.domain.account.Account;
import com.budgettracker.domain.account.AccountType;
import java.time.Instant;

public record AccountResponse(
    Integer id,
    String name,
    String bank,
    AccountType accountType,
    Instant createdAt,
    Instant updatedAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getBank(),
            account.getAccountType(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
