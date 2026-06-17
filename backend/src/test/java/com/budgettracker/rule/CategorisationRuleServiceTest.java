package com.budgettracker.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.domain.rule.CategorisationRule;
import com.budgettracker.domain.rule.CategorisationRuleRepository;
import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import com.budgettracker.tag.TagNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategorisationRuleServiceTest {

    @Mock
    private CategorisationRuleRepository ruleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private CategorisationRuleService ruleService;

    @Test
    void listsRules() {
        Category category = new Category("Dining", CategoryType.EXPENSE, true, 10);
        when(ruleRepository.findAllByOrderByPriorityAscIdAsc())
            .thenReturn(List.of(new CategorisationRule("coffee", category, null, true, 5)));

        assertThat(ruleService.listRules())
            .extracting(CategorisationRuleResponse::matchText)
            .containsExactly("coffee");
    }

    @Test
    void createsRule() {
        Category category = new Category("Dining", CategoryType.EXPENSE, true, 10);
        Tag tag = new Tag("Work", "#336699");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(tagRepository.findById(2)).thenReturn(Optional.of(tag));
        when(ruleRepository.save(any(CategorisationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ruleService.createRule(new CategorisationRuleRequest("coffee", 1, 2, true, 5));

        ArgumentCaptor<CategorisationRule> captor = ArgumentCaptor.forClass(CategorisationRule.class);
        verify(ruleRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchText()).isEqualTo("coffee");
        assertThat(captor.getValue().getPriority()).isEqualTo(5);
    }

    @Test
    void updatesRule() {
        Category oldCategory = new Category("Dining", CategoryType.EXPENSE, true, 10);
        Category newCategory = new Category("Groceries", CategoryType.EXPENSE, true, 20);
        CategorisationRule rule = new CategorisationRule("coffee", oldCategory, null, true, 5);
        when(ruleRepository.findById(1)).thenReturn(Optional.of(rule));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(newCategory));

        CategorisationRuleResponse response = ruleService.updateRule(
            1,
            new CategorisationRuleRequest("supermarket", 2, null, false, 1)
        );

        assertThat(response.matchText()).isEqualTo("supermarket");
        assertThat(response.active()).isFalse();
        assertThat(response.priority()).isEqualTo(1);
    }

    @Test
    void throwsWhenCategoryIsMissing() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.createRule(
            new CategorisationRuleRequest("coffee", 99, null, true, 1)
        )).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void throwsWhenTagIsMissing() {
        when(categoryRepository.findById(1))
            .thenReturn(Optional.of(new Category("Dining", CategoryType.EXPENSE, true, 10)));
        when(tagRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.createRule(
            new CategorisationRuleRequest("coffee", 1, 99, true, 1)
        )).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void deletesRule() {
        when(ruleRepository.existsById(1)).thenReturn(true);

        ruleService.deleteRule(1);

        verify(ruleRepository).deleteById(1);
    }
}
