package com.budgettracker.importing;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.importing.ImportBatch;
import com.budgettracker.domain.importing.ImportBatchRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionImportService {

    private final AccountRepository accountRepository;
    private final ImportBatchRepository importBatchRepository;
    private final TransactionRepository transactionRepository;
    private final List<CsvTransactionParser> parsers;

    public TransactionImportService(
        AccountRepository accountRepository,
        ImportBatchRepository importBatchRepository,
        TransactionRepository transactionRepository,
        List<CsvTransactionParser> parsers
    ) {
        this.accountRepository = accountRepository;
        this.importBatchRepository = importBatchRepository;
        this.transactionRepository = transactionRepository;
        this.parsers = parsers;
    }

    @Transactional
    public ImportSummaryResponse importTransactions(Integer accountId, MultipartFile file) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        byte[] content = readContent(file);
        if (new String(content, StandardCharsets.UTF_8).isBlank()) {
            return saveSummary(accountId, file.getOriginalFilename(), 0, 0, List.of(new ImportRowError(0, "CSV file is empty")));
        }

        CsvTransactionParser parser = selectParser(file.getOriginalFilename(), content);
        ParsedTransactionFile parsedFile = parse(parser, content);
        return saveParsedRows(accountId, file.getOriginalFilename(), parsedFile);
    }

    private ImportSummaryResponse saveParsedRows(
        Integer accountId,
        String originalFilename,
        ParsedTransactionFile parsedFile
    ) {
        ImportCandidateSelection candidates = selectImportCandidates(accountId, parsedFile.rows());

        ImportBatch importBatch = importBatchRepository.save(new ImportBatch(
            accountId,
            safeFilename(originalFilename),
            parsedFile.totalRows(),
            candidates.importableRows().size(),
            candidates.duplicateCount(),
            parsedFile.errors().size(),
            Instant.now()
        ));

        List<Transaction> transactions = candidates.importableRows().stream()
            .map(row -> new Transaction(
                accountId,
                row.transactionDate(),
                row.description(),
                row.rawDescription(),
                row.amount(),
                row.direction(),
                null,
                null,
                importBatch.getId()
            ))
            .toList();
        transactionRepository.saveAll(transactions);

        return new ImportSummaryResponse(
            parsedFile.totalRows(),
            transactions.size(),
            candidates.duplicateCount(),
            parsedFile.errors().size(),
            parsedFile.errors()
        );
    }

    private ImportCandidateSelection selectImportCandidates(Integer accountId, List<ParsedTransactionRow> rows) {
        List<ParsedTransactionRow> importableRows = new ArrayList<>();
        Set<DuplicateKey> seenInCurrentImport = new HashSet<>();
        int duplicateCount = 0;

        for (ParsedTransactionRow row : rows) {
            DuplicateKey key = DuplicateKey.from(accountId, row);
            if (seenInCurrentImport.contains(key) || transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
                accountId,
                row.transactionDate(),
                row.description(),
                row.amount()
            )) {
                duplicateCount++;
            } else {
                seenInCurrentImport.add(key);
                importableRows.add(row);
            }
        }

        return new ImportCandidateSelection(importableRows, duplicateCount);
    }

    private ImportSummaryResponse saveSummary(
        Integer accountId,
        String originalFilename,
        int totalRows,
        int importedCount,
        List<ImportRowError> errors
    ) {
        importBatchRepository.save(new ImportBatch(
            accountId,
            safeFilename(originalFilename),
            totalRows,
            importedCount,
            0,
            errors.size(),
            Instant.now()
        ));

        return new ImportSummaryResponse(totalRows, importedCount, 0, errors.size(), errors);
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new CsvImportException("Unable to read uploaded CSV file", exception);
        }
    }

    private CsvTransactionParser selectParser(String originalFilename, byte[] content) {
        List<String> header = readHeader(content);
        return parsers.stream()
            .filter(parser -> parser.supports(originalFilename, header))
            .findFirst()
            .orElseThrow(UnsupportedCsvFormatException::new);
    }

    private ParsedTransactionFile parse(CsvTransactionParser parser, byte[] content) {
        try {
            return parser.parse(new ByteArrayInputStream(content));
        } catch (IOException exception) {
            throw new CsvImportException("Unable to parse uploaded CSV file", exception);
        }
    }

    private List<String> readHeader(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        int lineEnd = text.indexOf('\n');
        String header = lineEnd >= 0 ? text.substring(0, lineEnd) : text;
        return Arrays.stream(header.replace("\r", "").split(",", -1))
            .map(String::trim)
            .toList();
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "transactions.csv";
        }

        return originalFilename;
    }

    private record ImportCandidateSelection(List<ParsedTransactionRow> importableRows, int duplicateCount) {
    }

    private record DuplicateKey(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount
    ) {

        static DuplicateKey from(Integer accountId, ParsedTransactionRow row) {
            return new DuplicateKey(
                accountId,
                row.transactionDate(),
                row.description(),
                row.amount().stripTrailingZeros()
            );
        }
    }
}
