package com.budgettracker.domain.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.ValidationTestSupport;
import org.junit.jupiter.api.Test;

class TagValidationTest extends ValidationTestSupport {

    @Test
    void requiresNameAndHexColor() {
        Tag tag = new Tag("", "green");

        assertThat(invalidProperties(tag)).contains("name", "color");
    }
}
