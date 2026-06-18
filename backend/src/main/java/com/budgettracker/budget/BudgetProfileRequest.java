package com.budgettracker.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BudgetProfileRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 500)
    String description,

    boolean active
) {
}
