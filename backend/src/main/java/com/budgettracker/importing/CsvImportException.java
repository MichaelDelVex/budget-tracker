package com.budgettracker.importing;

public class CsvImportException extends RuntimeException {

    public CsvImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
