package com.budgettracker.importing;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.category.CategoryResponse;
import com.budgettracker.domain.category.CategoryType;
import com.budgettracker.web.RestExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(ImportController.class)
@Import(RestExceptionHandler.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionImportService transactionImportService;

    @MockBean
    private CsvCategoryMappingService csvCategoryMappingService;

    @Test
    void importsTransactionsCsv() throws Exception {
        MockMultipartFile file = csvFile("Date,Description,Amount\n10/01/2026,Coffee,-4.50\n");
        when(transactionImportService.importTransactions(eq(1), any(MultipartFile.class))).thenReturn(new ImportSummaryResponse(
            1,
            1,
            0,
            0,
            List.of(),
            List.of(),
            List.of()
        ));

        mockMvc.perform(multipart("/api/imports/transactions")
                .file(file)
                .param("accountId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(1))
            .andExpect(jsonPath("$.importedCount").value(1))
            .andExpect(jsonPath("$.duplicateCount").value(0))
            .andExpect(jsonPath("$.failedCount").value(0))
            .andExpect(jsonPath("$.duplicates").isArray())
            .andExpect(jsonPath("$.unmatchedCategories").isArray());
    }

    @Test
    void returnsNotFoundWhenAccountDoesNotExist() throws Exception {
        MockMultipartFile file = csvFile("Date,Description,Amount\n");
        when(transactionImportService.importTransactions(eq(99), any(MultipartFile.class)))
            .thenThrow(new AccountNotFoundException(99));

        mockMvc.perform(multipart("/api/imports/transactions")
                .file(file)
                .param("accountId", "99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Account not found: 99"));
    }

    @Test
    void createsCsvCategoryMapping() throws Exception {
        when(csvCategoryMappingService.createMapping(any())).thenReturn(new CsvCategoryMappingResponse(
            "Parking & tolls",
            new CategoryResponse(12, "Parking", CategoryType.EXPENSE, false, true, 500, null, null)
        ));

        mockMvc.perform(post("/api/imports/csv-categories")
                .contentType("application/json")
                .content("""
                    {
                      "sourceName": "Parking & tolls",
                      "categoryName": "Parking",
                      "type": "EXPENSE"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceName").value("Parking & tolls"))
            .andExpect(jsonPath("$.category.name").value("Parking"));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
