package com.budgettracker.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class CategorisationRuleValidationTest extends ValidationTestSupport {

    @Test
    void requiresMatchTextCategoryAndNonNegativePriority() {
        CategorisationRule rule = new CategorisationRule("", null, null, true, -1);

        assertThat(invalidProperties(rule)).contains("matchText", "category", "priority");
    }
}
