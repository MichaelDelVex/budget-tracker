package com.budgettracker.budget;

import com.budgettracker.domain.budget.BudgetProfile;
import java.time.Instant;

public record BudgetProfileResponse(
    Integer id,
    String name,
    String description,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {

    public static BudgetProfileResponse from(BudgetProfile profile) {
        return new BudgetProfileResponse(
            profile.getId(),
            profile.getName(),
            profile.getDescription(),
            profile.isActive(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}
