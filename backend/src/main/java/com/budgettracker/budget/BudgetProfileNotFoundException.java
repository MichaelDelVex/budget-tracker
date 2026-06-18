package com.budgettracker.budget;

public class BudgetProfileNotFoundException extends RuntimeException {

    public BudgetProfileNotFoundException(Integer id) {
        super("Budget profile not found: " + id);
    }
}
