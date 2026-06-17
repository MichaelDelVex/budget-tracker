package com.budgettracker.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
    void requiresExistingAccount() {
        TransactionImportService importService = service();
        when(accountRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> importService.importTransactions(99, csvFile("Date,Description,Amount\n")))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void handlesEmptyCsvSafely() {
        TransactionImportService importService = service();
        when(accountRepository.existsById(1)).thenReturn(true);
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportSummaryResponse response = importService.importTransactions(1, csvFile(""));

        assertThat(response.totalRows()).isZero();
        assertThat(response.importedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.errors()).extracting(ImportRowError::message).contains("CSV file is empty");
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
}
