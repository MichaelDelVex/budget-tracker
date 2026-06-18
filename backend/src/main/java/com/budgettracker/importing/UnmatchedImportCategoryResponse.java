package com.budgettracker.importing;

import com.budgettracker.domain.category.CategoryType;

public record UnmatchedImportCategoryResponse(
    String name,
    CategoryType type,
    int rowCount
) {
}
