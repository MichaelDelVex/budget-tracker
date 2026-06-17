package com.budgettracker.category;

import com.budgettracker.domain.category.CategoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @NotNull
    CategoryType type,

    boolean defaultCategory,

    boolean active,

    @Min(0)
    int sortOrder
) {
}
