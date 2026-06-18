package com.budgettracker.importing;

import java.util.List;

public record ImportSummaryResponse(
    int totalRows,
    int importedCount,
    int duplicateCount,
    int failedCount,
    List<ImportRowError> errors,
    List<ImportDuplicateResponse> duplicates
) {
}
