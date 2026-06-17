package com.budgettracker.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.domain.rule.CategorisationRule;
import com.budgettracker.domain.rule.CategorisationRuleRepository;
import com.budgettracker.domain.tag.Tag;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategorisationRuleMatcherTest {

    @Mock
    private CategorisationRuleRepository ruleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void matchingRuleAppliesCategoryAndTag() {
        Category category = category(2);
        Tag tag = tag(3);
        when(ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc())
            .thenReturn(List.of(new CategorisationRule("coffee", category, tag, true, 10)));

        MatchedCategorisation result = matcher().match("Coffee Shop");

        assertThat(result.categoryId()).isEqualTo(2);
        assertThat(result.tagId()).isEqualTo(3);
    }

    @Test
    void matchingIsCaseInsensitive() {
        when(ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc())
            .thenReturn(List.of(new CategorisationRule("COFFEE", category(2), null, true, 10)));

        MatchedCategorisation result = matcher().match("morning coffee");

        assertThat(result.categoryId()).isEqualTo(2);
    }

    @Test
    void inactiveRulesAreIgnoredByRepositoryQuery() {
        when(ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc()).thenReturn(List.of());
        when(categoryRepository.findByNameIgnoreCaseAndActiveTrue("Uncategorised"))
            .thenReturn(Optional.of(category(9)));

        MatchedCategorisation result = matcher().match("Coffee Shop");

        assertThat(result.categoryId()).isEqualTo(9);
        assertThat(result.tagId()).isNull();
    }

    @Test
    void noMatchUsesUncategorisedWhenAvailable() {
        when(ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc()).thenReturn(List.of());
        when(categoryRepository.findByNameIgnoreCaseAndActiveTrue("Uncategorised"))
            .thenReturn(Optional.of(category(9)));

        MatchedCategorisation result = matcher().match("Unknown Merchant");

        assertThat(result.categoryId()).isEqualTo(9);
        assertThat(result.tagId()).isNull();
    }

    @Test
    void lowerPriorityRuleWins() {
        CategorisationRule highPriority = new CategorisationRule("coffee", category(2), null, true, 1);
        CategorisationRule lowPriority = new CategorisationRule("coffee", category(4), null, true, 10);
        when(ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc())
            .thenReturn(List.of(highPriority, lowPriority));

        MatchedCategorisation result = matcher().match("Coffee Shop");

        assertThat(result.categoryId()).isEqualTo(2);
    }

    private CategorisationRuleMatcher matcher() {
        return new CategorisationRuleMatcher(ruleRepository, categoryRepository);
    }

    private Category category(Integer id) {
        Category category = new Category("Category " + id, CategoryType.EXPENSE, false, id);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Tag tag(Integer id) {
        Tag tag = new Tag("Tag " + id, "#336699");
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }
}
