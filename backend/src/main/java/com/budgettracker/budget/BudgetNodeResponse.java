package com.budgettracker.budget;

import com.budgettracker.domain.budget.BudgetNode;
import java.math.BigDecimal;
import java.time.Instant;

public record BudgetNodeResponse(
    Integer id,
    Integer budgetProfileId,
    Integer parentNodeId,
    String name,
    BigDecimal percentage,
    Integer categoryId,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public static BudgetNodeResponse from(BudgetNode node) {
        return new BudgetNodeResponse(
            node.getId(),
            node.getBudgetProfileId(),
            node.getParentNodeId(),
            node.getName(),
            node.getPercentage(),
            node.getCategoryId(),
            node.getSortOrder(),
            node.getCreatedAt(),
            node.getUpdatedAt()
        );
    }
}
