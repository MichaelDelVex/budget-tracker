package com.budgettracker.importing;

public record ImportRowError(
    int rowNumber,
    String message
) {
}
