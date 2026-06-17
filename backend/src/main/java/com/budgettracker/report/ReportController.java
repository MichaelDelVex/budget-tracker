package com.budgettracker.report;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public SummaryReportResponse summary(@ModelAttribute ReportFilterRequest filter) {
        return reportService.summary(filter);
    }

    @GetMapping("/spending-by-category")
    public List<SpendingByCategoryResponse> spendingByCategory(@ModelAttribute ReportFilterRequest filter) {
        return reportService.spendingByCategory(filter);
    }

    @GetMapping("/income-vs-expenses")
    public List<IncomeVsExpensesResponse> incomeVsExpenses(@ModelAttribute ReportFilterRequest filter) {
        return reportService.incomeVsExpenses(filter);
    }

    @GetMapping("/property")
    public PropertyReportResponse property(@ModelAttribute ReportFilterRequest filter) {
        return reportService.property(filter);
    }
}
