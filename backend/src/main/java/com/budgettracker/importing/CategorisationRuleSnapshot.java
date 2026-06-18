package com.budgettracker.importing;

import java.util.List;
import java.util.Locale;

public class CategorisationRuleSnapshot {

    private final List<RuleMatch> rules;
    private final Integer uncategorisedCategoryId;

    public CategorisationRuleSnapshot(List<RuleMatch> rules, Integer uncategorisedCategoryId) {
        this.rules = List.copyOf(rules);
        this.uncategorisedCategoryId = uncategorisedCategoryId;
    }

    public MatchedCategorisation match(String description) {
        String normalisedDescription = description.toLowerCase(Locale.ROOT);
        for (RuleMatch rule : rules) {
            if (normalisedDescription.contains(rule.matchText())) {
                return new MatchedCategorisation(rule.categoryId(), rule.tagId());
            }
        }

        return new MatchedCategorisation(uncategorisedCategoryId, null);
    }

    public record RuleMatch(
        String matchText,
        Integer categoryId,
        Integer tagId
    ) {
        public RuleMatch {
            matchText = matchText.toLowerCase(Locale.ROOT);
        }
    }
}
