package com.budgettracker.importing;

import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.rule.CategorisationRule;
import com.budgettracker.domain.rule.CategorisationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategorisationRuleMatcher {

    private static final String UNCATEGORISED = "Uncategorised";

    private final CategorisationRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;

    public CategorisationRuleMatcher(
        CategorisationRuleRepository ruleRepository,
        CategoryRepository categoryRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public MatchedCategorisation match(String description) {
        return loadSnapshot().match(description);
    }

    @Transactional(readOnly = true)
    public CategorisationRuleSnapshot loadSnapshot() {
        return new CategorisationRuleSnapshot(
            ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc().stream()
                .map(this::toRuleMatch)
                .toList(),
            categoryRepository.findByNameIgnoreCaseAndActiveTrue(UNCATEGORISED)
                .map(category -> category.getId())
                .orElse(null)
        );
    }

    private CategorisationRuleSnapshot.RuleMatch toRuleMatch(CategorisationRule rule) {
        return new CategorisationRuleSnapshot.RuleMatch(
            rule.getMatchText(),
            rule.getCategory().getId(),
            rule.getTag() == null ? null : rule.getTag().getId()
        );
    }
}
