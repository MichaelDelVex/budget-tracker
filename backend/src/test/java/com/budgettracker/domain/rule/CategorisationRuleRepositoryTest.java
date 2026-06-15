package com.budgettracker.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategorisationRuleRepositoryTest {

    @Autowired
    private CategorisationRuleRepository ruleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void savesAndFindsEnabledRulesByPriority() {
        Category category = categoryRepository.findByName("Groceries").orElseThrow();
        String name = "Supermarket " + UUID.randomUUID();
        ruleRepository.save(new CategorisationRule(name, "supermarket", category, 10));

        assertThat(ruleRepository.findByEnabledTrueOrderByPriorityAsc())
            .extracting(CategorisationRule::getName)
            .contains(name);
    }
}
