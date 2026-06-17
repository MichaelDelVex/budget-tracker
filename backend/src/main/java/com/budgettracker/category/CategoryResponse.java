package com.budgettracker.category;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryType;
import java.time.Instant;

public record CategoryResponse(
    Integer id,
    String name,
    CategoryType type,
    boolean defaultCategory,
    boolean active,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getType(),
            category.isDefaultCategory(),
            category.isActive(),
            category.getSortOrder(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
