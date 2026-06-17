package com.budgettracker.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.budgettracker.domain.transaction.TransactionDirection;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NabCsvTransactionParserTest {

    private final NabCsvTransactionParser parser = new NabCsvTransactionParser();

    @Test
    void supportsNabStyleHeaders() {
        assertThat(parser.supports("nab.csv", List.of("Date", "Description", "Debit", "Credit"))).isTrue();
        assertThat(parser.supports("nab.csv", List.of("Transaction Date", "Description", "Debit Amount", "Credit Amount")))
            .isTrue();
    }

    @Test
    void parsesValidDebitAndCreditRows() throws Exception {
        ParsedTransactionFile parsed = parser.parse(csv("""
            Date,Description,Debit,Credit
            10/01/2026,Coffee,4.50,
            11/01/2026,Salary,,1000.00
            """));

        assertThat(parsed.totalRows()).isEqualTo(2);
        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).hasSize(2);
        assertThat(parsed.rows().get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(parsed.rows().get(0).amount()).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(parsed.rows().get(0).direction()).isEqualTo(TransactionDirection.EXPENSE);
        assertThat(parsed.rows().get(1).direction()).isEqualTo(TransactionDirection.INCOME);
    }

    @Test
    void normalisesSignedAmountRows() throws Exception {
        ParsedTransactionFile parsed = parser.parse(csv("""
            Date,Description,Amount
            10/01/2026,Coffee,-4.50
            11/01/2026,Refund,12.00
            """));

        assertThat(parsed.rows().get(0).amount()).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(parsed.rows().get(0).direction()).isEqualTo(TransactionDirection.EXPENSE);
        assertThat(parsed.rows().get(1).amount()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(parsed.rows().get(1).direction()).isEqualTo(TransactionDirection.INCOME);
    }

    @Test
    void handlesEmptyCsvSafely() throws Exception {
        ParsedTransactionFile parsed = parser.parse(csv(""));

        assertThat(parsed.totalRows()).isZero();
        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).extracting(ImportRowError::message).contains("CSV file is empty");
    }

    @Test
    void reportsInvalidRows() throws Exception {
        ParsedTransactionFile parsed = parser.parse(csv("""
            Date,Description,Amount
            not-a-date,Coffee,-4.50
            11/01/2026,Refund,not-money
            """));

        assertThat(parsed.totalRows()).isEqualTo(2);
        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).hasSize(2);
        assertThat(parsed.errors()).extracting(ImportRowError::message)
            .contains("Invalid transaction date", "Invalid amount");
    }

    private ByteArrayInputStream csv(String value) {
        return new ByteArrayInputStream(value.stripIndent().getBytes(StandardCharsets.UTF_8));
    }
}
