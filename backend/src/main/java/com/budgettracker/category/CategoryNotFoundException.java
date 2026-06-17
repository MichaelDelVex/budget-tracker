package com.budgettracker.category;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Integer id) {
        super("Category not found: " + id);
    }
}
