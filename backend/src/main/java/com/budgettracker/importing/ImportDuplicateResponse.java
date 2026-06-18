package com.budgettracker.importing;

public record ImportDuplicateResponse(
    ImportDuplicateTransactionResponse incoming,
    ImportDuplicateTransactionResponse matchedTransaction
) {
}
