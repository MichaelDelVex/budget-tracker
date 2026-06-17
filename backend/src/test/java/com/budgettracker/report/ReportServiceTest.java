package com.budgettracker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.category.Category;
import com.budgettracker.domain.category.CategoryRepository;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
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

    @Mock
    private TagRepository tagRepository;

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

    @Test
    void calculatesPropertyIncomeExpensesAndNetPosition() {
        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(
            transaction("Rent", "2000.00", TransactionDirection.INCOME, null, 1, LocalDate.of(2026, 1, 1), 10),
            transaction("Mortgage", "1200.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 2), 11),
            transaction("Insurance", "100.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 3), 12),
            transaction("Rates", "250.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 4), 13),
            transaction("Repairs", "300.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 5), 14),
            transaction("Manager", "150.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 6), 15),
            transaction("Other", "75.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 7), 16),
            transaction("Unknown", "999.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 8), 17)
        ));
        when(tagRepository.findAllById(List.of(10, 11, 12, 13, 14, 15, 16, 17))).thenReturn(List.of(
            tag(10, "Rental Income"),
            tag(11, "Mortgage"),
            tag(12, "Insurance"),
            tag(13, "Rates"),
            tag(14, "Repairs"),
            tag(15, "Property Management"),
            tag(16, "Other Property Expense"),
            tag(17, "Holiday")
        ));

        PropertyReportResponse response = reportService.property(filter());

        assertThat(response.rentalIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.mortgage()).isEqualByComparingTo("1200.00");
        assertThat(response.insurance()).isEqualByComparingTo("100.00");
        assertThat(response.rates()).isEqualByComparingTo("250.00");
        assertThat(response.repairs()).isEqualByComparingTo("300.00");
        assertThat(response.propertyManagementFees()).isEqualByComparingTo("150.00");
        assertThat(response.otherPropertyExpenses()).isEqualByComparingTo("75.00");
        assertThat(response.totalPropertyIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.totalPropertyExpenses()).isEqualByComparingTo("2075.00");
        assertThat(response.netPropertyPosition()).isEqualByComparingTo("-75.00");
    }

    @Test
    void ignoresPropertyTagsWithWrongDirectionAndUnknownTags() {
        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(
            transaction("Wrong direction rent", "2000.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 1), 10),
            transaction("Unknown", "50.00", TransactionDirection.EXPENSE, null, 1, LocalDate.of(2026, 1, 2), 99)
        ));
        when(tagRepository.findAllById(List.of(10, 99))).thenReturn(List.of(
            tag(10, "Rental Income"),
            tag(99, "Random")
        ));

        PropertyReportResponse response = reportService.property(filter());

        assertThat(response.totalPropertyIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalPropertyExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.netPropertyPosition()).isEqualByComparingTo("0.00");
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
        return transaction(description, amount, direction, categoryId, accountId, date, null);
    }

    private Transaction transaction(
        String description,
        String amount,
        TransactionDirection direction,
        Integer categoryId,
        Integer accountId,
        LocalDate date,
        Integer tagId
    ) {
        return new Transaction(
            accountId,
            date,
            description,
            description,
            new BigDecimal(amount),
            direction,
            categoryId,
            tagId,
            null
        );
    }

    private Category category(Integer id, String name) {
        Category category = new Category(name, CategoryType.EXPENSE, true, id);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Tag tag(Integer id, String name) {
        Tag tag = new Tag(name, "#336699");
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }
}
