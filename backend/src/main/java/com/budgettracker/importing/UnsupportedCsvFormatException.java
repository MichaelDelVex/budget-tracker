package com.budgettracker.importing;

public class UnsupportedCsvFormatException extends RuntimeException {

    public UnsupportedCsvFormatException() {
        super("Unsupported CSV format");
    }
}
