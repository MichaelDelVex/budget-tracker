package com.budgettracker.importing;

import com.budgettracker.domain.transaction.TransactionDirection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NabCsvTransactionParser implements CsvTransactionParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    @Override
    public boolean supports(String originalFilename, List<String> header) {
        Map<String, Integer> columns = indexHeader(header);
        return columns.containsKey("date")
            && hasAny(columns, "description", "details", "particulars")
            && (columns.containsKey("amount") || columns.containsKey("debit") || columns.containsKey("credit"));
    }

    @Override
    public ParsedTransactionFile parse(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new ParsedTransactionFile(0, List.of(), List.of(new ImportRowError(0, "CSV file is empty")));
            }

            List<String> header = parseCsvLine(headerLine);
            Map<String, Integer> columns = indexHeader(header);
            List<ParsedTransactionRow> rows = new ArrayList<>();
            List<ImportRowError> errors = new ArrayList<>();

            String line;
            int rowNumber = 1;
            int totalRows = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                totalRows++;
                List<String> values = parseCsvLine(line);
                try {
                    rows.add(parseRow(rowNumber, values, columns));
                } catch (IllegalArgumentException exception) {
                    errors.add(new ImportRowError(rowNumber, exception.getMessage()));
                }
            }

            if (totalRows == 0) {
                errors.add(new ImportRowError(0, "CSV file contains no transaction rows"));
            }

            return new ParsedTransactionFile(totalRows, rows, errors);
        }
    }

    private ParsedTransactionRow parseRow(int rowNumber, List<String> values, Map<String, Integer> columns) {
        LocalDate transactionDate = parseDate(requiredValue(values, columns, "date"), "Invalid transaction date");
        String description = firstPresent(values, columns, "description", "details", "particulars");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }

        NormalisedAmount amount = parseAmount(values, columns);
        return new ParsedTransactionRow(
            rowNumber,
            transactionDate,
            description.trim(),
            description,
            amount.amount(),
            amount.direction()
        );
    }

    private NormalisedAmount parseAmount(List<String> values, Map<String, Integer> columns) {
        String debit = value(values, columns, "debit");
        if (debit != null && !debit.isBlank()) {
            return new NormalisedAmount(parsePositiveAmount(debit), TransactionDirection.EXPENSE);
        }

        String credit = value(values, columns, "credit");
        if (credit != null && !credit.isBlank()) {
            return new NormalisedAmount(parsePositiveAmount(credit), TransactionDirection.INCOME);
        }

        String amountValue = value(values, columns, "amount");
        if (amountValue == null || amountValue.isBlank()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal signedAmount = parseDecimal(amountValue);
        if (signedAmount.signum() == 0) {
            throw new IllegalArgumentException("Amount must not be zero");
        }

        TransactionDirection direction = signedAmount.signum() > 0
            ? TransactionDirection.INCOME
            : TransactionDirection.EXPENSE;
        return new NormalisedAmount(signedAmount.abs(), direction);
    }

    private BigDecimal parsePositiveAmount(String value) {
        BigDecimal amount = parseDecimal(value);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        return amount;
    }

    private BigDecimal parseDecimal(String value) {
        try {
            String cleaned = value.trim()
                .replace("$", "")
                .replace(",", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }

    private LocalDate parseDate(String value, String errorMessage) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException(errorMessage);
    }

    private String requiredValue(List<String> values, Map<String, Integer> columns, String column) {
        String value = value(values, columns, column);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(column + " is required");
        }

        return value;
    }

    private String firstPresent(List<String> values, Map<String, Integer> columns, String... names) {
        for (String name : names) {
            String value = value(values, columns, name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private String value(List<String> values, Map<String, Integer> columns, String column) {
        Integer index = columns.get(column);
        if (index == null || index >= values.size()) {
            return null;
        }

        return values.get(index);
    }

    private Map<String, Integer> indexHeader(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            columns.put(normaliseHeader(header.get(i)), i);
        }
        return columns;
    }

    private boolean hasAny(Map<String, Integer> columns, String... names) {
        for (String name : names) {
            if (columns.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    private String normaliseHeader(String value) {
        String normalised = value.trim()
            .toLowerCase(Locale.ROOT)
            .replace("\uFEFF", "")
            .replace(" ", "_")
            .replace("-", "_")
            .replace("_", "");

        return switch (normalised) {
            case "transactiondate" -> "date";
            case "debitamount" -> "debit";
            case "creditamount" -> "credit";
            default -> normalised;
        };
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (character == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString());
        return values;
    }

    private record NormalisedAmount(BigDecimal amount, TransactionDirection direction) {
    }
}
