package com.budgettracker.rule;

public class CategorisationRuleNotFoundException extends RuntimeException {

    public CategorisationRuleNotFoundException(Integer id) {
        super("Categorisation rule not found: " + id);
    }
}
