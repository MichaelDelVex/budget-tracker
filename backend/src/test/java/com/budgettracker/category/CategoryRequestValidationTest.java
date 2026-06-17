package com.budgettracker.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class CategoryRequestValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameTypeAndNonNegativeSortOrder() {
        CategoryRequest request = new CategoryRequest("", null, false, true, -1);

        assertThat(invalidProperties(request)).contains("name", "type", "sortOrder");
    }
}
