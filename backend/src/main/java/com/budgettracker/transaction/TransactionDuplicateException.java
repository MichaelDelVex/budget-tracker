package com.budgettracker.transaction;

public class TransactionDuplicateException extends RuntimeException {

    public TransactionDuplicateException() {
        super("This transaction already exists.");
    }
}
