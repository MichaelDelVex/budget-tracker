package com.budgettracker.importing;

import com.budgettracker.domain.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CsvCategoryMappingRequest(
    @NotBlank
    @Size(max = 120)
    String sourceName,

    @NotBlank
    @Size(max = 120)
    String categoryName,

    @NotNull
    CategoryType type
) {
}
