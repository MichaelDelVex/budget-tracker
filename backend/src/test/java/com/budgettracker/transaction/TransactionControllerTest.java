package com.budgettracker.transaction;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.budgettracker.account.AccountNotFoundException;
import com.budgettracker.category.CategoryNotFoundException;
import com.budgettracker.domain.transaction.TransactionDirection;
import com.budgettracker.tag.TagNotFoundException;
import com.budgettracker.web.RestExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(RestExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void listsTransactionsWithFilters() throws Exception {
        PageRequest pageable = PageRequest.of(0, 25);
        when(transactionService.listTransactions(
            org.mockito.ArgumentMatchers.eq(new TransactionFilterRequest(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                1,
                2,
                3,
                TransactionDirection.EXPENSE,
                "coffee"
            )),
            org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(response()), pageable, 1));

        mockMvc.perform(get("/api/transactions")
                .param("dateFrom", "2026-01-01")
                .param("dateTo", "2026-01-31")
                .param("accountId", "1")
                .param("categoryId", "2")
                .param("tagId", "3")
                .param("direction", "EXPENSE")
                .param("search", "coffee")
                .param("page", "0")
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].description").value("Coffee"));
    }

    @Test
    void getsTransaction() throws Exception {
        when(transactionService.getTransaction(1)).thenReturn(response());

        mockMvc.perform(get("/api/transactions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Coffee"));
    }

    @Test
    void returnsNotFoundWhenTransactionIsMissing() throws Exception {
        when(transactionService.getTransaction(99)).thenThrow(new TransactionNotFoundException(99));

        mockMvc.perform(get("/api/transactions/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Transaction not found: 99"));
    }

    @Test
    void updatesTransaction() throws Exception {
        when(transactionService.updateTransaction(
            org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.any(TransactionUpdateRequest.class)
        )).thenReturn(response());

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accountId": 1,
                      "transactionDate": "2026-01-10",
                      "description": "Coffee",
                      "rawDescription": "COFFEE SHOP",
                      "amount": 4.50,
                      "direction": "EXPENSE",
                      "categoryId": 2,
                      "tagId": 3,
                      "importBatchId": null
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryId").value(2))
            .andExpect(jsonPath("$.tagId").value(3));
    }

    @Test
    void rejectsInvalidUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accountId": 0,
                      "transactionDate": null,
                      "description": "",
                      "rawDescription": "",
                      "amount": 0,
                      "direction": null,
                      "categoryId": 0,
                      "tagId": 0,
                      "importBatchId": 0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fields.accountId").exists())
            .andExpect(jsonPath("$.fields.transactionDate").exists())
            .andExpect(jsonPath("$.fields.description").exists())
            .andExpect(jsonPath("$.fields.rawDescription").exists())
            .andExpect(jsonPath("$.fields.amount").exists())
            .andExpect(jsonPath("$.fields.direction").exists())
            .andExpect(jsonPath("$.fields.categoryId").exists())
            .andExpect(jsonPath("$.fields.tagId").exists())
            .andExpect(jsonPath("$.fields.importBatchId").exists());
    }

    @Test
    void returnsClearErrorWhenUpdateAccountIsMissing() throws Exception {
        when(transactionService.updateTransaction(
            org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.any(TransactionUpdateRequest.class)
        )).thenThrow(new AccountNotFoundException(99));

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequestJson(99, 2, 3)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Account not found: 99"));
    }

    @Test
    void returnsClearErrorWhenUpdateCategoryIsMissing() throws Exception {
        when(transactionService.updateTransaction(
            org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.any(TransactionUpdateRequest.class)
        )).thenThrow(new CategoryNotFoundException(99));

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequestJson(1, 99, 3)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Category not found: 99"));
    }

    @Test
    void returnsClearErrorWhenUpdateTagIsMissing() throws Exception {
        when(transactionService.updateTransaction(
            org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.any(TransactionUpdateRequest.class)
        )).thenThrow(new TagNotFoundException(99));

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequestJson(1, 2, 99)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Tag not found: 99"));
    }

    @Test
    void returnsConflictWhenUpdateWouldCreateDuplicateTransaction() throws Exception {
        when(transactionService.updateTransaction(
            org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.any(TransactionUpdateRequest.class)
        )).thenThrow(new TransactionDuplicateException());

        mockMvc.perform(put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateRequestJson(1, 2, 3)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("This transaction already exists."));
    }

    @Test
    void deletesTransaction() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1);

        mockMvc.perform(delete("/api/transactions/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingMissingTransaction() throws Exception {
        doThrow(new TransactionNotFoundException(99)).when(transactionService).deleteTransaction(99);

        mockMvc.perform(delete("/api/transactions/99"))
            .andExpect(status().isNotFound());
    }

    private static TransactionResponse response() {
        Instant now = Instant.parse("2026-01-10T00:00:00Z");
        return new TransactionResponse(
            1,
            1,
            LocalDate.of(2026, 1, 10),
            "Coffee",
            "COFFEE SHOP",
            new BigDecimal("4.50"),
            TransactionDirection.EXPENSE,
            2,
            3,
            null,
            now,
            now
        );
    }

    private static String validUpdateRequestJson(Integer accountId, Integer categoryId, Integer tagId) {
        return """
            {
              "accountId": %d,
              "transactionDate": "2026-01-10",
              "description": "Coffee",
              "rawDescription": "COFFEE SHOP",
              "amount": 4.50,
              "direction": "EXPENSE",
              "categoryId": %d,
              "tagId": %d,
              "importBatchId": null
            }
            """.formatted(accountId, categoryId, tagId);
    }
}
