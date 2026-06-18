package com.budgettracker.account;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.budgettracker.domain.account.AccountType;
import com.budgettracker.web.RestExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@Import(RestExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    void listsAccounts() throws Exception {
        when(accountService.listAccounts()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Everyday"));
    }

    @Test
    void getsAccount() throws Exception {
        when(accountService.getAccount(1)).thenReturn(response());

        mockMvc.perform(get("/api/accounts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bank").value("Example Bank"));
    }

    @Test
    void returnsNotFoundWhenAccountIsMissing() throws Exception {
        when(accountService.getAccount(99)).thenThrow(new AccountNotFoundException(99));

        mockMvc.perform(get("/api/accounts/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Account not found: 99"));
    }

    @Test
    void createsAccount() throws Exception {
        when(accountService.createAccount(org.mockito.ArgumentMatchers.any(AccountRequest.class)))
            .thenReturn(response());

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Everyday",
                      "bank": "Example Bank",
                      "accountType": "CHECKING"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/accounts/1"))
            .andExpect(jsonPath("$.accountType").value("CHECKING"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "bank": "",
                      "accountType": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fields.name").exists())
            .andExpect(jsonPath("$.fields.bank").exists())
            .andExpect(jsonPath("$.fields.accountType").exists());
    }

    @Test
    void updatesAccount() throws Exception {
        when(accountService.updateAccount(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.any(AccountRequest.class)))
            .thenReturn(response());

        mockMvc.perform(put("/api/accounts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Everyday",
                      "bank": "Example Bank",
                      "accountType": "CHECKING"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Everyday"));
    }

    @Test
    void deletesAccount() throws Exception {
        doNothing().when(accountService).deleteAccount(1);

        mockMvc.perform(delete("/api/accounts/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingMissingAccount() throws Exception {
        doThrow(new AccountNotFoundException(99)).when(accountService).deleteAccount(99);

        mockMvc.perform(delete("/api/accounts/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletesAccountWithTransactions() throws Exception {
        doNothing().when(accountService).deleteAccountWithTransactions(1);

        mockMvc.perform(delete("/api/accounts/1/with-transactions"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenNukingMissingAccount() throws Exception {
        doThrow(new AccountNotFoundException(99)).when(accountService).deleteAccountWithTransactions(99);

        mockMvc.perform(delete("/api/accounts/99/with-transactions"))
            .andExpect(status().isNotFound());
    }

    private static AccountResponse response() {
        Instant now = Instant.parse("2026-06-15T00:00:00Z");
        return new AccountResponse(1, "Everyday", "Example Bank", AccountType.CHECKING, now, now);
    }
}
