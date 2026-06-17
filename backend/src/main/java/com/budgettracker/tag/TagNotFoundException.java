package com.budgettracker.tag;

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(Integer id) {
        super("Tag not found: " + id);
    }
}
