package com.budgettracker.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class CategorisationRuleValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameMatchTextCategoryAndNonNegativePriority() {
        CategorisationRule rule = new CategorisationRule("", "", null, -1);

        assertThat(invalidProperties(rule)).contains("name", "matchText", "category", "priority");
    }
}
