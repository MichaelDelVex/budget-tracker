package com.budgettracker.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.budgettracker.web.RestExceptionHandler;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import(RestExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void returnsSummaryReport() throws Exception {
        when(reportService.summary(any())).thenReturn(new SummaryReportResponse(
            new BigDecimal("1000.00"),
            new BigDecimal("250.00"),
            new BigDecimal("750.00"),
            new BigDecimal("75.00"),
            2
        ));

        mockMvc.perform(get("/api/reports/summary")
                .param("dateFrom", "2026-01-01")
                .param("dateTo", "2026-01-31")
                .param("accountId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome").value(1000.00))
            .andExpect(jsonPath("$.transactionCount").value(2));

        ArgumentCaptor<ReportFilterRequest> captor = ArgumentCaptor.forClass(ReportFilterRequest.class);
        org.mockito.Mockito.verify(reportService).summary(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().accountId()).isEqualTo(1);
    }

    @Test
    void returnsSpendingByCategoryReport() throws Exception {
        when(reportService.spendingByCategory(any())).thenReturn(List.of(new SpendingByCategoryResponse(
            2,
            "Dining",
            new BigDecimal("120.00"),
            new BigDecimal("48.00")
        )));

        mockMvc.perform(get("/api/reports/spending-by-category"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].categoryName").value("Dining"));
    }

    @Test
    void returnsIncomeVsExpensesReport() throws Exception {
        when(reportService.incomeVsExpenses(any())).thenReturn(List.of(new IncomeVsExpensesResponse(
            "2026-01",
            new BigDecimal("1000.00"),
            new BigDecimal("250.00"),
            new BigDecimal("750.00")
        )));

        mockMvc.perform(get("/api/reports/income-vs-expenses").param("groupBy", "MONTH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].period").value("2026-01"));
    }

    @Test
    void returnsPropertyReport() throws Exception {
        when(reportService.property(any())).thenReturn(new PropertyReportResponse(
            new BigDecimal("2000.00"),
            new BigDecimal("1200.00"),
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            new BigDecimal("300.00"),
            new BigDecimal("150.00"),
            new BigDecimal("75.00"),
            new BigDecimal("2000.00"),
            new BigDecimal("2075.00"),
            new BigDecimal("-75.00")
        ));

        mockMvc.perform(get("/api/reports/property")
                .param("dateFrom", "2026-01-01")
                .param("dateTo", "2026-01-31")
                .param("accountId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rentalIncome").value(2000.00))
            .andExpect(jsonPath("$.netPropertyPosition").value(-75.00));
    }
}
