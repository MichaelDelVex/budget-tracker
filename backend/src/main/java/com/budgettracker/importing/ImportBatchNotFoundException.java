package com.budgettracker.importing;

public class ImportBatchNotFoundException extends RuntimeException {

    public ImportBatchNotFoundException(Integer id) {
        super("Import batch not found: " + id);
    }
}
