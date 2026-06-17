package com.budgettracker.rule;

import com.budgettracker.domain.rule.CategorisationRule;
import java.time.Instant;

public record CategorisationRuleResponse(
    Integer id,
    String matchText,
    Integer categoryId,
    Integer tagId,
    boolean active,
    int priority,
    Instant createdAt,
    Instant updatedAt
) {

    public static CategorisationRuleResponse from(CategorisationRule rule) {
        return new CategorisationRuleResponse(
            rule.getId(),
            rule.getMatchText(),
            rule.getCategory().getId(),
            rule.getTag() == null ? null : rule.getTag().getId(),
            rule.isActive(),
            rule.getPriority(),
            rule.getCreatedAt(),
            rule.getUpdatedAt()
        );
    }
}
