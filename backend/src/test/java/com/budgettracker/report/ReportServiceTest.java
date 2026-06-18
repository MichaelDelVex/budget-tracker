package com.budgettracker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import com.budgettracker.domain.transaction.CategorySpendingView;
import com.budgettracker.domain.transaction.DailyIncomeExpenseView;
import com.budgettracker.domain.transaction.Transaction;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.domain.transaction.TransactionRepository;
import com.budgettracker.domain.transaction.TransactionSummaryView;
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
    private TagRepository tagRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void calculatesSummary() {
        when(transactionRepository.summarizeTransactions(
            null,
            null,
            null,
            TransactionDirection.INCOME,
            TransactionDirection.EXPENSE
        )).thenReturn(summary("1000.00", "250.00", 2));

        SummaryReportResponse response = reportService.summary(filter());

        assertThat(response.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("250.00");
        assertThat(response.netSavings()).isEqualByComparingTo("750.00");
        assertThat(response.savingsPercentage()).isEqualByComparingTo("75.00");
        assertThat(response.transactionCount()).isEqualTo(2);
    }

    @Test
    void calculatesSpendingByCategory() {
        when(transactionRepository.summarizeSpendingByCategory(
            null,
            null,
            null,
            TransactionDirection.EXPENSE
        )).thenReturn(List.of(
            categorySpending(2, "Groceries", "60.00"),
            categorySpending(3, "Dining", "40.00")
        ));

        List<SpendingByCategoryResponse> response = reportService.spendingByCategory(filter());

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().categoryName()).isEqualTo("Groceries");
        assertThat(response.getFirst().totalAmount()).isEqualByComparingTo("60.00");
        assertThat(response.getFirst().percentageOfExpenses()).isEqualByComparingTo("60.00");
    }

    @Test
    void groupsIncomeVsExpensesByMonth() {
        when(transactionRepository.summarizeIncomeVsExpensesByDate(
            null,
            null,
            null,
            TransactionDirection.INCOME,
            TransactionDirection.EXPENSE
        )).thenReturn(List.of(
            daily(LocalDate.of(2026, 1, 10), "1000.00", "0.00"),
            daily(LocalDate.of(2026, 1, 11), "0.00", "500.00"),
            daily(LocalDate.of(2026, 2, 10), "1100.00", "0.00")
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
        when(transactionRepository.summarizeIncomeVsExpensesByDate(
            null,
            null,
            null,
            TransactionDirection.INCOME,
            TransactionDirection.EXPENSE
        )).thenReturn(List.of(
            daily(LocalDate.of(2026, 1, 14), "0.00", "10.00"),
            daily(LocalDate.of(2026, 1, 15), "0.00", "20.00")
        ));

        List<IncomeVsExpensesResponse> response = reportService.incomeVsExpenses(
            new ReportFilterRequest(null, null, null, ReportGroupBy.FORTNIGHT)
        );

        assertThat(response)
            .extracting(IncomeVsExpensesResponse::period)
            .containsExactly("2026-01 F1", "2026-01 F2");
    }

    @Test
    void emptySummaryReturnsZeroValues() {
        when(transactionRepository.summarizeTransactions(
            null,
            null,
            null,
            TransactionDirection.INCOME,
            TransactionDirection.EXPENSE
        )).thenReturn(summary(null, null, 0));

        SummaryReportResponse response = reportService.summary(filter());

        assertThat(response.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.netSavings()).isEqualByComparingTo("0.00");
        assertThat(response.savingsPercentage()).isEqualByComparingTo("0.00");
        assertThat(response.transactionCount()).isZero();
    }

    @Test
    void emptySpendingByCategoryReturnsEmptyList() {
        when(transactionRepository.summarizeSpendingByCategory(
            null,
            null,
            null,
            TransactionDirection.EXPENSE
        )).thenReturn(List.of());

        assertThat(reportService.spendingByCategory(filter())).isEmpty();
    }

    @Test
    void emptyIncomeVsExpensesReturnsEmptyList() {
        when(transactionRepository.summarizeIncomeVsExpensesByDate(
            null,
            null,
            null,
            TransactionDirection.INCOME,
            TransactionDirection.EXPENSE
        )).thenReturn(List.of());

        assertThat(reportService.incomeVsExpenses(filter())).isEmpty();
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

    private Tag tag(Integer id, String name) {
        Tag tag = new Tag(name, "#336699");
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    private TransactionSummaryView summary(String totalIncome, String totalExpenses, long transactionCount) {
        return new TransactionSummaryView() {
            @Override
            public BigDecimal getTotalIncome() {
                return totalIncome == null ? null : new BigDecimal(totalIncome);
            }

            @Override
            public BigDecimal getTotalExpenses() {
                return totalExpenses == null ? null : new BigDecimal(totalExpenses);
            }

            @Override
            public long getTransactionCount() {
                return transactionCount;
            }
        };
    }

    private CategorySpendingView categorySpending(Integer categoryId, String categoryName, String totalAmount) {
        return new CategorySpendingView() {
            @Override
            public Integer getCategoryId() {
                return categoryId;
            }

            @Override
            public String getCategoryName() {
                return categoryName;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal(totalAmount);
            }
        };
    }

    private DailyIncomeExpenseView daily(LocalDate transactionDate, String totalIncome, String totalExpenses) {
        return new DailyIncomeExpenseView() {
            @Override
            public LocalDate getTransactionDate() {
                return transactionDate;
            }

            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal(totalIncome);
            }

            @Override
            public BigDecimal getTotalExpenses() {
                return new BigDecimal(totalExpenses);
            }
        };
    }
}
