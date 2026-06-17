package com.budgettracker.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void listsTransactionsWithFiltersAndPagination() {
        Pageable pageable = PageRequest.of(0, 25);
        when(transactionRepository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(sampleTransaction()), pageable, 1));

        TransactionFilterRequest filter = new TransactionFilterRequest(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            1,
            2,
            3,
            TransactionDirection.EXPENSE,
            "coffee"
        );

        assertThat(transactionService.listTransactions(filter, pageable).getContent())
            .extracting(TransactionResponse::description)
            .containsExactly("Coffee");
    }

    @Test
    void getsTransactionById() {
        when(transactionRepository.findById(1)).thenReturn(Optional.of(sampleTransaction()));

        assertThat(transactionService.getTransaction(1).direction()).isEqualTo(TransactionDirection.EXPENSE);
    }

    @Test
    void throwsWhenTransactionIsMissing() {
        when(transactionRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(99))
            .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void updatesTransactionIncludingCategoryAndTag() {
        Transaction transaction = sampleTransaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.updateTransaction(
            1,
            new TransactionUpdateRequest(
                4,
                LocalDate.of(2026, 2, 1),
                "Updated",
                "Updated raw",
                new BigDecimal("99.99"),
                TransactionDirection.INCOME,
                5,
                6,
                7
            )
        );

        assertThat(response.description()).isEqualTo("Updated");
        assertThat(response.categoryId()).isEqualTo(5);
        assertThat(response.tagId()).isEqualTo(6);
    }

    @Test
    void deletesTransaction() {
        when(transactionRepository.existsById(1)).thenReturn(true);

        transactionService.deleteTransaction(1);

        verify(transactionRepository).deleteById(1);
    }

    @Test
    void throwsWhenDeletingMissingTransaction() {
        when(transactionRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.deleteTransaction(99))
            .isInstanceOf(TransactionNotFoundException.class);
    }

    private static Transaction sampleTransaction() {
        return new Transaction(
            1,
            LocalDate.of(2026, 1, 10),
            "Coffee",
            "COFFEE SHOP",
            new BigDecimal("4.50"),
            TransactionDirection.EXPENSE,
            2,
            3,
            null
        );
    }
}
