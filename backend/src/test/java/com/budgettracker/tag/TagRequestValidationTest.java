package com.budgettracker.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class TagRequestValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameAndHexColor() {
        TagRequest request = new TagRequest("", "blue");

        assertThat(invalidProperties(request)).contains("name", "color");
    }
}
