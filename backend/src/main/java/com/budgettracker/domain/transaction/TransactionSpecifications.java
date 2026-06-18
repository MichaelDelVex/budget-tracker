package com.budgettracker.domain.transaction;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> transactionDateOnOrAfter(LocalDate dateFrom) {
        return (root, query, builder) -> dateFrom == null
            ? null
            : builder.greaterThanOrEqualTo(root.get("transactionDate"), dateFrom);
    }

    public static Specification<Transaction> transactionDateOnOrBefore(LocalDate dateTo) {
        return (root, query, builder) -> dateTo == null
            ? null
            : builder.lessThanOrEqualTo(root.get("transactionDate"), dateTo);
    }

    public static Specification<Transaction> accountIdEquals(Integer accountId) {
        return equalsIfPresent("accountId", accountId);
    }

    public static Specification<Transaction> categoryIdEquals(Integer categoryId) {
        return equalsIfPresent("categoryId", categoryId);
    }

    public static Specification<Transaction> uncategorisedOnly(boolean enabled, Integer uncategorisedCategoryId) {
        return (root, query, builder) -> {
            if (!enabled) {
                return null;
            }

            if (uncategorisedCategoryId == null) {
                return builder.isNull(root.get("categoryId"));
            }

            return builder.or(
                builder.isNull(root.get("categoryId")),
                builder.equal(root.get("categoryId"), uncategorisedCategoryId)
            );
        };
    }

    public static Specification<Transaction> tagIdEquals(Integer tagId) {
        return equalsIfPresent("tagId", tagId);
    }

    public static Specification<Transaction> directionEquals(TransactionDirection direction) {
        return equalsIfPresent("direction", direction);
    }

    public static Specification<Transaction> searchDescriptions(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.or(
                builder.like(builder.lower(root.get("description")), pattern),
                builder.like(builder.lower(root.get("rawDescription")), pattern)
            );
        };
    }

    private static <T> Specification<Transaction> equalsIfPresent(String field, T value) {
        return (root, query, builder) -> value == null
            ? null
            : builder.equal(root.get(field), value);
    }
}
