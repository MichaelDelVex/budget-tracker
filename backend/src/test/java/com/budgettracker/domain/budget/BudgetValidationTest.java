package com.budgettracker.domain.budget;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BudgetValidationTest extends ValidationTestSupport {

    @Test
    void validatesBudgetProfileFields() {
        BudgetProfile profile = new BudgetProfile("", "Description", false);

        assertThat(invalidProperties(profile)).contains("name");
    }

    @Test
    void validatesBudgetNodeFields() {
        BudgetNode node = new BudgetNode(
            0,
            0,
            "",
            new BigDecimal("101.00"),
            0,
            -1
        );

        assertThat(invalidProperties(node))
            .contains("budgetProfileId", "parentNodeId", "name", "percentage", "categoryId", "sortOrder");
    }
}
