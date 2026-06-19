package com.budgettracker.importing;

import com.budgettracker.domain.category.CategoryType;
import java.util.List;

public record UnmatchedImportCategoryResponse(
    String name,
    CategoryType type,
    int rowCount,
    List<UnmatchedImportCategoryRowResponse> rows
) {
}
