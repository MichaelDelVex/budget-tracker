package com.budgettracker.rule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategorisationRuleRequest(
    @NotBlank
    @Size(max = 255)
    String matchText,

    @NotNull
    @Min(1)
    Integer categoryId,

    @Min(1)
    Integer tagId,

    boolean active,

    @Min(0)
    int priority
) {
}
