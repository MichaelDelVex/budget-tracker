package com.budgettracker.importing;

import com.budgettracker.category.CategoryResponse;

public record CsvCategoryMappingResponse(
    String sourceName,
    CategoryResponse category
) {
}
