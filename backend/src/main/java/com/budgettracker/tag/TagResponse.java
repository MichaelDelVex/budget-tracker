package com.budgettracker.tag;

import com.budgettracker.domain.tag.Tag;
import java.time.Instant;

public record TagResponse(
    Integer id,
    String name,
    String color,
    Instant createdAt,
    Instant updatedAt
) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            tag.getCreatedAt(),
            tag.getUpdatedAt()
        );
    }
}
