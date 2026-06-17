package com.budgettracker.transaction;

import static com.budgettracker.domain.transaction.TransactionSpecifications.accountIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.categoryIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.directionEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.searchDescriptions;
import static com.budgettracker.domain.transaction.TransactionSpecifications.tagIdEquals;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrAfter;
import static com.budgettracker.domain.transaction.TransactionSpecifications.transactionDateOnOrBefore;

import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
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
}
