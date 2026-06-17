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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionImportService.class);

    private final AccountRepository accountRepository;
    private final ImportBatchRepository importBatchRepository;
    private final TransactionRepository transactionRepository;
    private final CategorisationRuleMatcher categorisationRuleMatcher;
    private final List<CsvTransactionParser> parsers;
    private final JdbcTemplate jdbcTemplate;

    public TransactionImportService(
        AccountRepository accountRepository,
        ImportBatchRepository importBatchRepository,
        TransactionRepository transactionRepository,
        CategorisationRuleMatcher categorisationRuleMatcher,
        List<CsvTransactionParser> parsers,
        JdbcTemplate jdbcTemplate
    ) {
        this.accountRepository = accountRepository;
        this.importBatchRepository = importBatchRepository;
        this.transactionRepository = transactionRepository;
        this.categorisationRuleMatcher = categorisationRuleMatcher;
        this.parsers = parsers;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportSummaryResponse importTransactions(Integer accountId, MultipartFile file) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        byte[] content = readContent(file);
        if (new String(content, StandardCharsets.UTF_8).isBlank()) {
            return saveSummary(
                accountId,
                file.getOriginalFilename(),
                0,
                0,
                0,
                List.of(new ImportRowError(0, "CSV file is empty"))
            );
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

        ImportBatch importBatch = importBatchRepository.saveAndFlush(new ImportBatch(
            accountId,
            safeFilename(originalFilename),
            parsedFile.totalRows(),
            0,
            candidates.duplicateCount(),
            parsedFile.errors().size(),
            Instant.now()
        ));

        ImportInsertResult insertResult = insertTransactions(accountId, importBatch.getId(), candidates.importableRows());
        int duplicateCount = candidates.duplicateCount() + insertResult.duplicateCount();
        importBatch.updateCounts(insertResult.importedCount(), duplicateCount, parsedFile.errors().size());

        ImportSummaryResponse response = new ImportSummaryResponse(
            parsedFile.totalRows(),
            insertResult.importedCount(),
            duplicateCount,
            parsedFile.errors().size(),
            parsedFile.errors()
        );
        logImportSummary(accountId, originalFilename, response);
        return response;
    }

    private Transaction toTransaction(Integer accountId, Integer importBatchId, ParsedTransactionRow row) {
        String description = normaliseDescription(row.description());
        MatchedCategorisation categorisation = categorisationRuleMatcher.match(description);
        return new Transaction(
            accountId,
            row.transactionDate(),
            description,
            row.rawDescription(),
            row.amount(),
            row.direction(),
            categorisation.categoryId(),
            categorisation.tagId(),
            importBatchId
        );
    }

    private ImportCandidateSelection selectImportCandidates(Integer accountId, List<ParsedTransactionRow> rows) {
        List<ParsedTransactionRow> importableRows = new ArrayList<>();
        Set<DuplicateKey> seenInCurrentImport = new HashSet<>();
        int duplicateCount = 0;

        for (ParsedTransactionRow row : rows) {
            String description = normaliseDescription(row.description());
            DuplicateKey key = DuplicateKey.from(accountId, row, description);
            if (seenInCurrentImport.contains(key) || transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
                accountId,
                row.transactionDate(),
                description,
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

    private ImportInsertResult insertTransactions(
        Integer accountId,
        Integer importBatchId,
        List<ParsedTransactionRow> rows
    ) {
        int importedCount = 0;
        int duplicateCount = 0;

        for (ParsedTransactionRow row : rows) {
            try {
                insertTransaction(toTransaction(accountId, importBatchId, row));
                importedCount++;
            } catch (DataIntegrityViolationException exception) {
                if (!isDuplicateConstraintViolation(exception)) {
                    throw exception;
                }
                duplicateCount++;
            }
        }

        return new ImportInsertResult(importedCount, duplicateCount);
    }

    private void insertTransaction(Transaction transaction) {
        jdbcTemplate.update(
            """
            INSERT INTO transaction_record (
                account_id,
                transaction_date,
                description,
                raw_description,
                amount,
                direction,
                category_id,
                tag_id,
                import_batch_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            transaction.getAccountId(),
            transaction.getTransactionDate().toString(),
            transaction.getDescription(),
            transaction.getRawDescription(),
            transaction.getAmount(),
            transaction.getDirection().name(),
            transaction.getCategoryId(),
            transaction.getTagId(),
            transaction.getImportBatchId()
        );
    }

    private boolean isDuplicateConstraintViolation(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message == null) {
            message = exception.getMessage();
        }

        return message != null
            && (message.contains("ux_transactions_duplicate_key")
                || message.contains("transaction_record.account_id, transaction_record.transaction_date, transaction_record.description, transaction_record.amount"));
    }

    private ImportSummaryResponse saveSummary(
        Integer accountId,
        String originalFilename,
        int totalRows,
        int importedCount,
        int duplicateCount,
        List<ImportRowError> errors
    ) {
        importBatchRepository.save(new ImportBatch(
            accountId,
            safeFilename(originalFilename),
            totalRows,
            importedCount,
            duplicateCount,
            errors.size(),
            Instant.now()
        ));

        ImportSummaryResponse response = new ImportSummaryResponse(
            totalRows,
            importedCount,
            duplicateCount,
            errors.size(),
            errors
        );
        logImportSummary(accountId, originalFilename, response);
        return response;
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

    private String normaliseDescription(String description) {
        return description.trim().replaceAll("\\s+", " ");
    }

    private void logImportSummary(Integer accountId, String originalFilename, ImportSummaryResponse response) {
        LOGGER.info(
            "CSV import completed accountId={} filename={} totalRows={} importedCount={} duplicateCount={} failedCount={}",
            accountId,
            safeFilename(originalFilename),
            response.totalRows(),
            response.importedCount(),
            response.duplicateCount(),
            response.failedCount()
        );
    }

    private record ImportCandidateSelection(List<ParsedTransactionRow> importableRows, int duplicateCount) {
    }

    private record ImportInsertResult(int importedCount, int duplicateCount) {
    }

    private record DuplicateKey(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount
    ) {

        static DuplicateKey from(Integer accountId, ParsedTransactionRow row, String description) {
            return new DuplicateKey(
                accountId,
                row.transactionDate(),
                description,
                row.amount().stripTrailingZeros()
            );
        }
    }
}
