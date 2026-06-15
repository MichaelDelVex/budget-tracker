package com.budgettracker.domain.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findsSeededDefaultCategories() {
        assertThat(categoryRepository.findByDefaultCategoryTrueOrderBySortOrderAsc())
            .extracting(Category::getName)
            .contains("Salary", "Groceries", "Uncategorised");
    }

    @Test
    void savesAndFindsCategoryByName() {
        String name = "Subscriptions " + UUID.randomUUID();
        categoryRepository.save(new Category(name, CategoryType.EXPENSE, false, 200));

        assertThat(categoryRepository.findByName(name))
            .isPresent()
            .get()
            .extracting(Category::getType)
            .isEqualTo(CategoryType.EXPENSE);
    }
}
