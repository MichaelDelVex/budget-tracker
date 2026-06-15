package com.budgettracker.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class InstantStringConverter implements AttributeConverter<Instant, String> {

    private static final DateTimeFormatter SQLITE_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(Instant value) {
        return value == null ? null : value.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        if (value.contains("T")) {
            return Instant.parse(value);
        }

        return LocalDateTime.parse(value, SQLITE_TIMESTAMP).toInstant(ZoneOffset.UTC);
    }
}
