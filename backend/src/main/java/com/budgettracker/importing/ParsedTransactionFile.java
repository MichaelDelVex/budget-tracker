package com.budgettracker.importing;

import java.util.List;

public record ParsedTransactionFile(
    int totalRows,
    List<ParsedTransactionRow> rows,
    List<ImportRowError> errors
) {
}
