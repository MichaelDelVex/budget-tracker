package com.budgettracker.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.importing.ImportBatch;
import com.budgettracker.domain.importing.ImportBatchRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CsvTransactionParser parser;

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

        ArgumentCaptor<Iterable<Transaction>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void skipsExistingTransactionAndIncrementsDuplicateCount() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));
        when(transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            1,
            row.transactionDate(),
            row.description(),
            row.amount()
        )).thenReturn(true);

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isZero();
        assertThat(response.duplicateCount()).isEqualTo(1);
        verify(transactionRepository).saveAll(List.of());
    }

    @Test
    void differentAccountDoesNotCountAsDuplicate() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        when(accountRepository.existsById(2)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));
        when(transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            eq(2),
            eq(row.transactionDate()),
            eq(row.description()),
            eq(row.amount())
        )).thenReturn(false);

        ImportSummaryResponse response = importService.importTransactions(2, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isZero();
    }

    @Test
    void sameDateAndDescriptionWithDifferentAmountImports() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow row = row(LocalDate.of(2026, 1, 10), "Coffee", "5.50");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(1, List.of(row), List.of()));
        when(transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            1,
            row.transactionDate(),
            row.description(),
            row.amount()
        )).thenReturn(false);

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
        when(transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            1,
            row.transactionDate(),
            row.description(),
            row.amount()
        )).thenReturn(false);

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
    void reimportingSameCsvImportsZeroNewRows() throws Exception {
        TransactionImportService importService = service();
        stubImportBatchSave();
        ParsedTransactionRow first = row(LocalDate.of(2026, 1, 10), "Coffee", "4.50");
        ParsedTransactionRow second = row(LocalDate.of(2026, 1, 11), "Lunch", "12.00");
        when(accountRepository.existsById(1)).thenReturn(true);
        when(parser.supports(any(), any())).thenReturn(true);
        when(parser.parse(any())).thenReturn(new ParsedTransactionFile(2, List.of(first, second), List.of()));
        when(transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            eq(1),
            any(LocalDate.class),
            any(String.class),
            any(BigDecimal.class)
        )).thenReturn(true);

        ImportSummaryResponse response = importService.importTransactions(1, csvFile("Date,Description,Amount\n"));

        assertThat(response.importedCount()).isZero();
        assertThat(response.duplicateCount()).isEqualTo(2);
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

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private TransactionImportService service() {
        return new TransactionImportService(
            accountRepository,
            importBatchRepository,
            transactionRepository,
            List.of(parser)
        );
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

    private void stubImportBatchSave() {
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
