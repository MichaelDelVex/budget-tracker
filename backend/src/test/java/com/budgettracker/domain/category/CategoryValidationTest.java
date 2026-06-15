package com.budgettracker.domain.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class CategoryValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameTypeAndNonNegativeSortOrder() {
        Category category = new Category("", null, false, -1);

        assertThat(invalidProperties(category)).contains("name", "type", "sortOrder");
    }
}
