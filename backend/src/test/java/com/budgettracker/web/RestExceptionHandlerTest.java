package com.budgettracker.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void returnsConsistentMessageForMalformedRequestBodies() {
        var response = handler.handleUnreadableRequest();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(ApiError.of("Request body is invalid or malformed."));
    }

    @Test
    void returnsClearMessageForOversizedUploads() {
        var response = handler.handleUploadTooLarge();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isEqualTo(ApiError.of("CSV file is too large. Use a file up to 5 MB."));
    }

    @Test
    void returnsSpecificMessageForDuplicateAccountNames() {
        var response = handler.handleConflict(conflict("UNIQUE constraint failed: account.name"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(ApiError.of("An account with this name already exists."));
    }

    @Test
    void returnsSpecificMessageForDuplicateCategoryNames() {
        var response = handler.handleConflict(conflict("UNIQUE constraint failed: category.name"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(ApiError.of("A category with this name already exists."));
    }

    @Test
    void returnsSpecificMessageForDuplicateTagNames() {
        var response = handler.handleConflict(conflict("UNIQUE constraint failed: tag.name"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(ApiError.of("A tag with this name already exists."));
    }

    @Test
    void returnsSpecificMessageForDuplicateTransactions() {
        var response = handler.handleConflict(conflict(
            "UNIQUE constraint failed: transaction_record.account_id, "
                + "transaction_record.transaction_date, transaction_record.description, transaction_record.amount"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(ApiError.of("This transaction already exists."));
    }

    @Test
    void returnsSpecificMessageForReferencedResources() {
        var response = handler.handleConflict(conflict("FOREIGN KEY constraint failed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(ApiError.of(
            "Cannot delete or update this resource because it is still referenced."
        ));
    }

    @Test
    void returnsConsistentMessageForUnexpectedErrors() {
        var response = handler.handleUnexpected(new IllegalStateException("Database exploded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(ApiError.of("Something went wrong. Check the server logs for details."));
    }

    private static DataIntegrityViolationException conflict(String message) {
        return new DataIntegrityViolationException("Constraint failed", new RuntimeException(message));
    }
}
