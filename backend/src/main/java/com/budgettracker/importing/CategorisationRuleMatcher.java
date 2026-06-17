package com.budgettracker.importing;

import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.rule.CategorisationRule;
import com.budgettracker.domain.rule.CategorisationRuleRepository;
import java.util.Locale;
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
        String normalisedDescription = description.toLowerCase(Locale.ROOT);
        for (CategorisationRule rule : ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc()) {
            String matchText = rule.getMatchText().toLowerCase(Locale.ROOT);
            if (normalisedDescription.contains(matchText)) {
                return new MatchedCategorisation(
                    rule.getCategory().getId(),
                    rule.getTag() == null ? null : rule.getTag().getId()
                );
            }
        }

        return categoryRepository.findByNameIgnoreCaseAndActiveTrue(UNCATEGORISED)
            .map(category -> new MatchedCategorisation(category.getId(), null))
            .orElse(new MatchedCategorisation(null, null));
    }
}
