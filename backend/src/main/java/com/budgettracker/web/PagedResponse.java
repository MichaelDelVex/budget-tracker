package com.budgettracker.web;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}
