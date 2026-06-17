package com.budgettracker.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class CategorisationRuleRequestValidationTest extends ValidationTestSupport {

    @Test
    void requiresMatchTextCategoryAndNonNegativePriority() {
        CategorisationRuleRequest request = new CategorisationRuleRequest("", 0, 0, true, -1);

        assertThat(invalidProperties(request)).contains("matchText", "categoryId", "tagId", "priority");
    }
}
