package com.budgettracker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void calculatesSummary() {
        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(
            transaction("Salary", "1000.00", TransactionDirection.INCOME, null, 1, LocalDate.of(2026, 1, 10)),
            transaction("Groceries", "250.00", TransactionDirection.EXPENSE, 2, 1, LocalDate.of(2026, 1, 11))
        ));

        SummaryReportResponse response = reportService.summary(filter());

        assertThat(response.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("250.00");
        assertThat(response.netSavings()).isEqualByComparingTo("750.00");
        assertThat(response.savingsPercentage()).isEqualByComparingTo("75.00");
        assertThat(response.transactionCount()).isEqualTo(2);
    }

    @Test
    void calculatesSpendingByCategory() {
        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(
            transaction("Groceries", "60.00", TransactionDirection.EXPENSE, 2, 1, LocalDate.of(2026, 1, 10)),
            transaction("Dining", "40.00", TransactionDirection.EXPENSE, 3, 1, LocalDate.of(2026, 1, 11))
        ));
        when(categoryRepository.findAllById(List.of(2, 3))).thenReturn(List.of(
            category(2, "Groceries"),
            category(3, "Dining")
        ));

        List<SpendingByCategoryResponse> response = reportService.spendingByCategory(filter());

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().categoryName()).isEqualTo("Groceries");
        assertThat(response.getFirst().totalAmount()).isEqualByComparingTo("60.00");
        assertThat(response.getFirst().percentageOfExpenses()).isEqualByComparingTo("60.00");
    }

    @Test
    void groupsIncomeVsExpensesByMonth() {
        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(
            transaction("Salary", "1000.00", TransactionDirection.INCOME, null, 1, LocalDate.of(2026, 1, 10)),
            transaction("Rent", "500.00", TransactionDirection.EXPENSE, 2, 1, LocalDate.of(2026, 1, 11)),
            transaction("Salary", "1100.00", TransactionDirection.INCOME, null, 1, LocalDate.of(2026, 2, 10))
        ));

        List<IncomeVsExpensesResponse> response = reportService.incomeVsExpenses(filter());

        assertThat(response)
            .extracting(IncomeVsExpensesResponse::period)
            .containsExactly("2026-01", "2026-02");
        assertThat(response.getFirst().totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.getFirst().totalExpenses()).isEqualByComparingTo("500.00");
    }

    @Test
    void supportsFortnightGrouping() {
        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(
            transaction("Early", "10.00", TransactionDirection.EXPENSE, 2, 1, LocalDate.of(2026, 1, 14)),
            transaction("Late", "20.00", TransactionDirection.EXPENSE, 2, 1, LocalDate.of(2026, 1, 15))
        ));

        List<IncomeVsExpensesResponse> response = reportService.incomeVsExpenses(
            new ReportFilterRequest(null, null, null, ReportGroupBy.FORTNIGHT)
        );

        assertThat(response)
            .extracting(IncomeVsExpensesResponse::period)
            .containsExactly("2026-01 F1", "2026-01 F2");
    }

    private ReportFilterRequest filter() {
        return new ReportFilterRequest(null, null, null, ReportGroupBy.MONTH);
    }

    private Transaction transaction(
        String description,
        String amount,
        TransactionDirection direction,
        Integer categoryId,
        Integer accountId,
        LocalDate date
    ) {
        return new Transaction(
            accountId,
            date,
            description,
            description,
            new BigDecimal(amount),
            direction,
            categoryId,
            null,
            null
        );
    }

    private Category category(Integer id, String name) {
        Category category = new Category(name, CategoryType.EXPENSE, true, id);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
