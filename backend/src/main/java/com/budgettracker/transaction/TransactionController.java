package com.budgettracker.transaction;

import com.budgettracker.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public PagedResponse<TransactionResponse> listTransactions(
        @ModelAttribute TransactionFilterRequest filter,
        @PageableDefault(size = 25, sort = "transactionDate") Pageable pageable
    ) {
        return PagedResponse.from(transactionService.listTransactions(filter, pageable));
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable Integer id) {
        return transactionService.getTransaction(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        return transactionService.createTransaction(request);
    }

    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(
        @PathVariable Integer id,
        @Valid @RequestBody TransactionUpdateRequest request
    ) {
        return transactionService.updateTransaction(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Integer id) {
        transactionService.deleteTransaction(id);
    }
}
