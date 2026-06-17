package com.budgettracker.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
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
    void returnsConsistentMessageForUnexpectedErrors() {
        var response = handler.handleUnexpected();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(ApiError.of("Something went wrong. Check the server logs for details."));
    }
}
