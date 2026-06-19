package com.budgettracker.transaction;

import static com.budgettracker.domain.transaction.TransactionSpecifications.accountIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.categoryIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.directionEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.searchDescriptions;
import static com.budgettracker.domain.transaction.TransactionSpecifications.tagIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrAfter;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrBefore;
import static com.budgettracker.domain.transaction.TransactionSpecifications.uncategorisedOnly;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.domain.account.AccountRepository;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.importing.ImportBatchRepository;
import com.budgettracker.domain.tag.TagRepository;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionRepository;
import com.budgettracker.importing.ImportBatchNotFoundException;
import com.budgettracker.tag.TagNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final String UNCATEGORISED = "Uncategorised";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ImportBatchRepository importBatchRepository;

    public TransactionService(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        TagRepository tagRepository,
        ImportBatchRepository importBatchRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.importBatchRepository = importBatchRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(TransactionFilterRequest filter, Pageable pageable) {
        return transactionRepository.findAll(toSpecification(filter), pageable)
            .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Integer id) {
        return transactionRepository.findById(id)
            .map(TransactionResponse::from)
            .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        validateReferences(request.accountId(), request.categoryId(), request.tagId(), null);
        validateDuplicateKey(
            request.accountId(),
            request.transactionDate(),
            request.description(),
            request.amount()
        );

        Transaction transaction = new Transaction(
            request.accountId(),
            request.transactionDate(),
            request.description(),
            request.rawDescription(),
            request.amount(),
            request.direction(),
            request.categoryId(),
            request.tagId(),
            null
        );

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse updateTransaction(Integer id, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new TransactionNotFoundException(id));

        validateReferences(request.accountId(), request.categoryId(), request.tagId(), request.importBatchId());
        validateDuplicateKey(transaction, request);

        transaction.update(
            request.accountId(),
            request.transactionDate(),
            request.description(),
            request.rawDescription(),
            request.amount(),
            request.direction(),
            request.categoryId(),
            request.tagId(),
            request.importBatchId()
        );

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void deleteTransaction(Integer id) {
        if (!transactionRepository.existsById(id)) {
            throw new TransactionNotFoundException(id);
        }

        transactionRepository.deleteById(id);
    }

    private Specification<Transaction> toSpecification(TransactionFilterRequest filter) {
        return Specification
            .where(transactionDateOnOrAfter(filter.dateFrom()))
            .and(transactionDateOnOrBefore(filter.dateTo()))
            .and(accountIdEquals(filter.accountId()))
            .and(categoryIdEquals(filter.categoryId()))
            .and(uncategorisedOnly(Boolean.TRUE.equals(filter.uncategorisedOnly()), uncategorisedCategoryId(filter)))
            .and(tagIdEquals(filter.tagId()))
            .and(directionEquals(filter.direction()))
            .and(searchDescriptions(filter.search()));
    }

    private Integer uncategorisedCategoryId(TransactionFilterRequest filter) {
        if (!Boolean.TRUE.equals(filter.uncategorisedOnly())) {
            return null;
        }

        return categoryRepository.findByNameIgnoreCaseAndActiveTrue(UNCATEGORISED)
            .map(category -> category.getId())
            .orElse(null);
    }

    private void validateReferences(Integer accountId, Integer categoryId, Integer tagId, Integer importBatchId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }

        if (tagId != null && !tagRepository.existsById(tagId)) {
            throw new TagNotFoundException(tagId);
        }

        if (importBatchId != null && !importBatchRepository.existsById(importBatchId)) {
            throw new ImportBatchNotFoundException(importBatchId);
        }
    }

    private void validateDuplicateKey(
        Integer accountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount
    ) {
        if (transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmount(
            accountId,
            transactionDate,
            description,
            amount
        )) {
            throw new TransactionDuplicateException();
        }
    }

    private void validateDuplicateKey(Transaction transaction, TransactionUpdateRequest request) {
        if (!duplicateKeyChanged(transaction, request)) {
            return;
        }

        if (transactionRepository.existsByAccountIdAndTransactionDateAndDescriptionAndAmountAndIdNot(
            request.accountId(),
            request.transactionDate(),
            request.description(),
            request.amount(),
            transaction.getId()
        )) {
            throw new TransactionDuplicateException();
        }
    }

    private boolean duplicateKeyChanged(Transaction transaction, TransactionUpdateRequest request) {
        return !Objects.equals(transaction.getAccountId(), request.accountId())
            || !Objects.equals(transaction.getTransactionDate(), request.transactionDate())
            || !Objects.equals(transaction.getDescription(), request.description())
            || moneyChanged(transaction.getAmount(), request.amount());
    }

    private boolean moneyChanged(BigDecimal current, BigDecimal requested) {
        return current.compareTo(requested) != 0;
    }
}
