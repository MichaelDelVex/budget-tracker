package com.budgettracker.importing;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.domain.importing.ImportBatch;
import com.budgettracker.domain.importing.ImportBatchRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionDuplicateMatchView;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    private final CategoryRepository categoryRepository;
    private final ImportBatchRepository importBatchRepository;
    private final TransactionRepository transactionRepository;
    private final CategorisationRuleMatcher categorisationRuleMatcher;
    private final List<CsvTransactionParser> parsers;
    private final JdbcTemplate jdbcTemplate;

    public TransactionImportService(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ImportBatchRepository importBatchRepository,
        TransactionRepository transactionRepository,
        CategorisationRuleMatcher categorisationRuleMatcher,
        List<CsvTransactionParser> parsers,
        JdbcTemplate jdbcTemplate
    ) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
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
        CsvCategorySelection csvCategories = selectCsvCategories(parsedFile.rows());

        ImportBatch importBatch = importBatchRepository.saveAndFlush(new ImportBatch(
            accountId,
            safeFilename(originalFilename),
            parsedFile.totalRows(),
            0,
            candidates.duplicateCount(),
            parsedFile.errors().size(),
            Instant.now()
        ));

        ImportInsertResult insertResult = insertTransactions(
            accountId,
            importBatch.getId(),
            candidates.importableRows(),
            csvCategories.categoryIdsByRowNumber()
        );
        int duplicateCount = candidates.duplicateCount() + insertResult.duplicateCount();
        importBatch.updateCounts(insertResult.importedCount(), duplicateCount, parsedFile.errors().size());

        ImportSummaryResponse response = new ImportSummaryResponse(
            parsedFile.totalRows(),
            insertResult.importedCount(),
            duplicateCount,
            parsedFile.errors().size(),
            parsedFile.errors(),
            candidates.duplicates(),
            csvCategories.unmatchedCategories()
        );
        logImportSummary(accountId, originalFilename, response);
        return response;
    }

    private Transaction toTransaction(
        Integer accountId,
        Integer importBatchId,
        ParsedTransactionRow row,
        CategorisationRuleSnapshot ruleSnapshot,
        Map<Integer, Integer> csvCategoryIdsByRowNumber
    ) {
        String description = normaliseDescription(row.description());
        Integer csvCategoryId = csvCategoryIdsByRowNumber.get(row.rowNumber());
        MatchedCategorisation categorisation = csvCategoryId == null
            ? ruleSnapshot.match(description)
            : new MatchedCategorisation(csvCategoryId, null);
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
        Map<DuplicateKey, ImportDuplicateTransactionResponse> existingMatches = existingDuplicateMatches(accountId, rows);
        Map<DuplicateKey, ImportDuplicateTransactionResponse> seenInCurrentImport = new HashMap<>();
        List<ImportDuplicateResponse> duplicates = new ArrayList<>();
        int duplicateCount = 0;

        for (ParsedTransactionRow row : rows) {
            String description = normaliseDescription(row.description());
            DuplicateKey key = DuplicateKey.from(accountId, row, description);
            ImportDuplicateTransactionResponse matchedTransaction = existingMatches.get(key);
            if (matchedTransaction == null) {
                matchedTransaction = seenInCurrentImport.get(key);
            }

            if (matchedTransaction != null) {
                duplicateCount++;
                duplicates.add(new ImportDuplicateResponse(
                    duplicateResponse(null, row.rowNumber(), row.transactionDate(), description, row.rawDescription(), row.amount(), row.direction(), null, null),
                    matchedTransaction
                ));
            } else {
                seenInCurrentImport.put(
                    key,
                    duplicateResponse(null, row.rowNumber(), row.transactionDate(), description, row.rawDescription(), row.amount(), row.direction(), null, null)
                );
                importableRows.add(row);
            }
        }

        return new ImportCandidateSelection(importableRows, duplicateCount, duplicates);
    }

    private Map<DuplicateKey, ImportDuplicateTransactionResponse> existingDuplicateMatches(
        Integer accountId,
        List<ParsedTransactionRow> rows
    ) {
        if (rows.isEmpty()) {
            return Map.of();
        }

        LocalDate dateFrom = rows.stream()
            .map(ParsedTransactionRow::transactionDate)
            .min(LocalDate::compareTo)
            .orElseThrow();
        LocalDate dateTo = rows.stream()
            .map(ParsedTransactionRow::transactionDate)
            .max(LocalDate::compareTo)
            .orElseThrow();

        Map<DuplicateKey, ImportDuplicateTransactionResponse> existingMatches = new HashMap<>();
        for (TransactionDuplicateMatchView match : transactionRepository.findDuplicateMatchesForAccountAndDateRange(
            accountId,
            dateFrom,
            dateTo
        )) {
            existingMatches.put(DuplicateKey.fromExisting(accountId, match), duplicateResponse(
                match.getId(),
                null,
                match.getTransactionDate(),
                match.getDescription(),
                match.getRawDescription(),
                match.getAmount(),
                match.getDirection(),
                match.getCategoryId(),
                match.getTagId()
            ));
        }

        return existingMatches;
    }

    private ImportInsertResult insertTransactions(
        Integer accountId,
        Integer importBatchId,
        List<ParsedTransactionRow> rows,
        Map<Integer, Integer> csvCategoryIdsByRowNumber
    ) {
        int importedCount = 0;
        int duplicateCount = 0;
        CategorisationRuleSnapshot ruleSnapshot = categorisationRuleMatcher.loadSnapshot();

        for (ParsedTransactionRow row : rows) {
            try {
                insertTransaction(toTransaction(accountId, importBatchId, row, ruleSnapshot, csvCategoryIdsByRowNumber));
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
            errors,
            List.of(),
            List.of()
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

    private CsvCategorySelection selectCsvCategories(List<ParsedTransactionRow> rows) {
        Map<CategoryKey, Integer> categoriesByNameAndType = new HashMap<>();
        for (Category category : categoryRepository.findAllByOrderBySortOrderAscNameAsc()) {
            if (category.isActive()) {
                categoriesByNameAndType.put(CategoryKey.from(category.getName(), category.getType()), category.getId());
            }
        }

        Map<Integer, Integer> categoryIdsByRowNumber = new HashMap<>();
        Map<CategoryKey, Integer> unmatchedCategoryCounts = new TreeMap<>((left, right) -> {
            int nameComparison = String.CASE_INSENSITIVE_ORDER.compare(left.name(), right.name());
            return nameComparison != 0 ? nameComparison : left.type().compareTo(right.type());
        });
        Map<CategoryKey, String> unmatchedCategoryDisplayNames = new HashMap<>();
        for (ParsedTransactionRow row : rows) {
            if (row.csvCategory() == null || row.csvCategory().isBlank()) {
                continue;
            }

            CategoryType type = categoryType(row.direction());
            Integer categoryId = categoriesByNameAndType.get(CategoryKey.from(row.csvCategory(), type));
            if (categoryId == null) {
                CategoryKey key = CategoryKey.from(row.csvCategory(), type);
                unmatchedCategoryDisplayNames.putIfAbsent(key, row.csvCategory().trim());
                unmatchedCategoryCounts.merge(key, 1, Integer::sum);
            } else {
                categoryIdsByRowNumber.put(row.rowNumber(), categoryId);
            }
        }

        return new CsvCategorySelection(
            categoryIdsByRowNumber,
            unmatchedCategoryCounts.entrySet().stream()
                .map(entry -> new UnmatchedImportCategoryResponse(
                    unmatchedCategoryDisplayNames.get(entry.getKey()),
                    entry.getKey().type(),
                    entry.getValue()
                ))
                .toList()
        );
    }

    private CategoryType categoryType(TransactionDirection direction) {
        return direction == TransactionDirection.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
    }

    private ImportDuplicateTransactionResponse duplicateResponse(
        Integer id,
        Integer rowNumber,
        LocalDate transactionDate,
        String description,
        String rawDescription,
        BigDecimal amount,
        TransactionDirection direction,
        Integer categoryId,
        Integer tagId
    ) {
        return new ImportDuplicateTransactionResponse(
            id,
            rowNumber,
            transactionDate,
            description,
            rawDescription,
            amount,
            direction,
            categoryId,
            tagId
        );
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

    private record ImportCandidateSelection(
        List<ParsedTransactionRow> importableRows,
        int duplicateCount,
        List<ImportDuplicateResponse> duplicates
    ) {
    }

    private record ImportInsertResult(int importedCount, int duplicateCount) {
    }

    private record CsvCategorySelection(
        Map<Integer, Integer> categoryIdsByRowNumber,
        List<UnmatchedImportCategoryResponse> unmatchedCategories
    ) {
    }

    private record CategoryKey(String name, CategoryType type) {

        static CategoryKey from(String name, CategoryType type) {
            return new CategoryKey(name.trim().toLowerCase(Locale.ROOT), type);
        }
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

        static DuplicateKey fromExisting(Integer accountId, TransactionDuplicateMatchView key) {
            return new DuplicateKey(
                accountId,
                key.getTransactionDate(),
                key.getDescription(),
                key.getAmount().stripTrailingZeros()
            );
        }
    }
}
