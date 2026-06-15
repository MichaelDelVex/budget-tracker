package com.budgettracker.web;

import java.util.Map;

public record ApiError(
    String message,
    Map<String, String> fields
) {

    public static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
