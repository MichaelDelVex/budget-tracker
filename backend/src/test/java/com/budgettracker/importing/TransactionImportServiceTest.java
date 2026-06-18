package com.budgettracker.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.importing.ImportBatch;
import com.budgettracker.domain.importing.ImportBatchRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDuplicateKeyView;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class TransactionImportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategorisationRuleMatcher categorisationRuleMatcher;

    @Mock
    private CsvTransactionParser parser;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void importsParsedTransactionsAndReturnsSummary() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        MockMultipartFile file = csvFile("Date,Description,Amount\n10/01/2026,Coffee,-4.50\n");
        ParsedTransactionRow row = new ParsedTransactionRow(
            2,
            LocalDate.of(2026, 1, 10),
            "Coffee",
            "Coffee",
            new BigDecimal("4.50"),
            TransactionDirection.EXPENSE
        );
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(2, List.of(row), List.of(
            new ImportRowError(3, "Invalid amount")
        )));

        ImportSummaryResponse response = importService.importTransactions(1, file);

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);

        verify(jdbcTemplate).update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void appliesMatchedCategoryAndTagToImportedTransactions() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));
        when(categorisationRuleMatcher.loadSnapshot()).thenReturn(new CategorisationRuleSnapshot(
            List.of(new CategorisationRuleSnapshot.RuleMatch("Coffee", 2, 3)),
            null
        ));

        importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        verify(jdbcTemplate).update(
            any(String.class),
            eq(1),
            eq("2026-01-10"),
            eq("Coffee"),
            eq("Coffee"),
            eq(new BigDecimal("4.50")),
            eq("EXPENSE"),
            eq(2),
            eq(3),
            eq(7)
        );
    }

    @Test
    void skipsExistingTransactionAndIncrementsDuplicateCount() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));
        when(transactionRepository.findDuplicateKeysForAccountAndDateRange(
            1,
            row.transactionDate(),
            row.transactionDate()
        )).thenReturn(List.of(duplicateKey(row.transactionDate(), row.description(), row.amount())));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isZero();
        assertThat(response.duplicateCount()).isEqualTo(1);
        verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void differentAccountDoesNotCountAsDuplicate() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        when(accountRepository.existsById(2)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));

        ImportSummaryResponse response = importService.importTransactions(2, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isZero();
        verify(transactionRepository).findDuplicateKeysForAccountAndDateRange(
            2,
            row.transactionDate(),
            row.transactionDate()
        );
    }

    @Test
    void sameDateAndDescriptionWithDifferentAmountImports() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "5.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isZero();
    }

    @Test
    void sameDescriptionAndAmountWithDifferentDateImports() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 11), "Coffee", "4.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isZero();
    }

    @Test
    void duplicateRowsWithinSameCsvAreSkipped() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow first = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        ParsedTransactionRow second = row(LocalDate.of(2026, 1, 10), "Coffee", "4.500");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(2, List.of(first, second), List.of()));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isEqualTo(1);
    }

    @Test
    void normalisesDescriptionForDuplicateChecksAndStorage() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = new ParsedTransactionRow(
            2,
            LocalDate.of(2026, 1, 10),
            "  Coffee   Shop  ",
            "  Coffee   Shop  ",
            new BigDecimal("4.50"),
            TransactionDirection.EXPENSE
        );
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isEqualTo(1);
        verify(jdbcTemplate).update(
            any(String.class),
            eq(1),
            eq("2026-01-10"),
            eq("Coffee Shop"),
            eq("  Coffee   Shop  "),
            eq(new BigDecimal("4.50")),
            eq("EXPENSE"),
            eq(null),
            eq(null),
            eq(7)
        );
    }

    @Test
    void loadsCategorisationRulesOnceForImportBatch() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow first = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        ParsedTransactionRow second = row(LocalDate.of(2026, 1, 11), "Lunch", "12.00");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(2, List.of(first, second), List.of()));

        importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        verify(categorisationRuleMatcher, times(1)).loadSnapshot();
    }

    @Test
    void reimportingSameCsvImportsZeroNewRows() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow first = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        ParsedTransactionRow second = row(LocalDate.of(2026, 1, 11), "Lunch", "12.00");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(2, List.of(first, second), List.of()));
        when(transactionRepository.findDuplicateKeysForAccountAndDateRange(
            1,
            first.transactionDate(),
            second.transactionDate()
        )).thenReturn(List.of(
            duplicateKey(first.transactionDate(), first.description(), first.amount()),
            duplicateKey(second.transactionDate(), second.description(), second.amount())
        ));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isZero();
        assertThat(response.duplicateCount()).isEqualTo(2);
    }

    @Test
    void preloadsExistingDuplicateKeysOnceForDateRange() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow first = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        ParsedTransactionRow second = row(LocalDate.of(2026, 1, 12), "Lunch", "12.00");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(2, List.of(first, second), List.of()));

        importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        verify(transactionRepository, times(1)).findDuplicateKeysForAccountAndDateRange(
            1,
            LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 1, 12)
        );
        verify(transactionRepository, never()).existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void countsDuplicateConstraintConflictAsDuplicate() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));
        when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new DuplicateKeyException(
                "UNIQUE constraint failed: transaction_record.account_id, transaction_record.transaction_date, transaction_record.description, transaction_record.amount"
            ));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isZero();
        assertThat(response.duplicateCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
    }

    @Test
    void requiresExistingAccount() {
        TransactionImportService importService = service();
        when(accountRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> importService.importTransactions(99, csvFile("Date,Description,Amount\n")))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void handlesEmptyCsvSafely() {
        TransactionImportService importService = service();
        stubImportBatchSave();
        when(accountRepository.existsById(1)).thenReturn(true);

        ImportSummaryResponse response = importService.importTransactions(1, csvFile(""));

        assertThat(response.totalRows()).isZero();
        assertThat(response.importedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.errors()).extracting(ImportRowError::message).contains("CSV file is empty");
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void logsImportSummaryWithoutTransactionDetails(CapturedOutput output) throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Private Store", "4.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));

        importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(output.getAll())
            .contains("CSV import completed accountId=1 filename=transactions.csv totalRows=1 importedCount=1 duplicateCount=0 failedCount=0")
            .doesNotContain("Private Store")
            .doesNotContain("4.50");
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private TransactionImportService service() {
        TransactionImportService service = new TransactionImportService(
            accountRepository,
            importBatchRepository,
            transactionRepository,
            categorisationRuleMatcher,
            List.of(parser),
            jdbcTemplate
        );
        lenient().when(categorisationRuleMatcher.loadSnapshot())
            .thenReturn(new CategorisationRuleSnapshot(List.of(), null));
        return service;
    }

    private ParsedTransactionRow row(LocalDate date, String description, String amount) {
        return new ParsedTransactionRow(
            2,
            date,
            description,
            description,
            new BigDecimal(amount),
            TransactionDirection.EXPENSE
        );
    }

    private TransactionDuplicateKeyView duplicateKey(LocalDate date, String description, BigDecimal amount) {
        return new TransactionDuplicateKeyView() {
            @Override
            public LocalDate getTransactionDate() {
                return date;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public BigDecimal getAmount() {
                return amount;
            }
        };
    }

    private void stubImportBatchSave() {
        lenient().when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(importBatchRepository.saveAndFlush(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch importBatch = invocation.getArgument(0);
            ReflectionTestUtils.setField(importBatch, "id", 7);
            return importBatch;
        });
    }
}
