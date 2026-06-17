package com.budgettracker.transaction;

import static com.budgettracker.domain.transaction.TransactionSpecifications.accountIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.categoryIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.directionEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.searchDescriptions;
import static com.budgettracker.domain.transaction.TransactionSpecifications.tagIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrAfter;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrBefore;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

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
    public TransactionResponse updateTransaction(Integer id, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new TransactionNotFoundException(id));

        validateReferences(request);

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
            .and(tagIdEquals(filter.tagId()))
            .and(directionEquals(filter.direction()))
            .and(searchDescriptions(filter.search()));
    }

    private void validateReferences(TransactionUpdateRequest request) {
        if (!accountRepository.existsById(request.accountId())) {
            throw new AccountNotFoundException(request.accountId());
        }

        if (request.categoryId() != null && !categoryRepository.existsById(request.categoryId())) {
            throw new CategoryNotFoundException(request.categoryId());
        }

        if (request.tagId() != null && !tagRepository.existsById(request.tagId())) {
            throw new TagNotFoundException(request.tagId());
        }

        if (request.importBatchId() != null && !importBatchRepository.existsById(request.importBatchId())) {
            throw new ImportBatchNotFoundException(request.importBatchId());
        }
    }
}
