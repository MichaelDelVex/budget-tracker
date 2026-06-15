package com.budgettracker.account;

import com.budgettracker.domain.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 120) String bank,
    @NotNull AccountType accountType
) {
}
