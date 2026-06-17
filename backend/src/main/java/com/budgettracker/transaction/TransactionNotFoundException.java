package com.budgettracker.transaction;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Integer id) {
        super("Transaction not found: " + id);
    }
}
