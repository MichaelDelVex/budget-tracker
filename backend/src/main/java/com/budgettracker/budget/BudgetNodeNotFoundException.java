package com.budgettracker.budget;

public class BudgetNodeNotFoundException extends RuntimeException {

    public BudgetNodeNotFoundException(Integer id) {
        super("Budget node not found: " + id);
    }
}
