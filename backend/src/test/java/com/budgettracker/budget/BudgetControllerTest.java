package com.budgettracker.budget;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.budgettracker.web.RestExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BudgetController.class)
@Import(RestExceptionHandler.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

    @Test
    void listsProfiles() throws Exception {
        when(budgetService.listProfiles()).thenReturn(List.of(profileResponse()));

        mockMvc.perform(get("/api/budgets/profiles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Default"));
    }

    @Test
    void createsProfile() throws Exception {
        when(budgetService.createProfile(any(BudgetProfileRequest.class))).thenReturn(profileResponse());

        mockMvc.perform(post("/api/budgets/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileBody()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/budgets/profiles/1"))
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void rejectsInvalidProfileRequest() throws Exception {
        mockMvc.perform(post("/api/budgets/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "description": "Main plan",
                      "active": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void updatesAndDeletesProfile() throws Exception {
        when(budgetService.updateProfile(eq(1), any(BudgetProfileRequest.class))).thenReturn(profileResponse());
        doNothing().when(budgetService).deleteProfile(1);

        mockMvc.perform(put("/api/budgets/profiles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileBody()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Default"));

        mockMvc.perform(delete("/api/budgets/profiles/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void listsNodes() throws Exception {
        when(budgetService.listNodes(1)).thenReturn(List.of(nodeResponse()));

        mockMvc.perform(get("/api/budgets/profiles/1/nodes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Savings"));
    }

    @Test
    void createsNode() throws Exception {
        when(budgetService.createNode(eq(1), any(BudgetNodeRequest.class))).thenReturn(nodeResponse());

        mockMvc.perform(post("/api/budgets/profiles/1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nodeBody()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/budgets/nodes/10"))
            .andExpect(jsonPath("$.percentage").value(40.00));
    }

    @Test
    void rejectsInvalidNodeRequest() throws Exception {
        mockMvc.perform(post("/api/budgets/profiles/1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "parentNodeId": null,
                      "name": "",
                      "percentage": 101,
                      "categoryId": 0,
                      "sortOrder": -1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists())
            .andExpect(jsonPath("$.fields.percentage").exists())
            .andExpect(jsonPath("$.fields.categoryId").exists())
            .andExpect(jsonPath("$.fields.sortOrder").exists());
    }

    @Test
    void updatesAndDeletesNode() throws Exception {
        when(budgetService.updateNode(eq(10), any(BudgetNodeRequest.class))).thenReturn(nodeResponse());
        doNothing().when(budgetService).deleteNode(10);

        mockMvc.perform(put("/api/budgets/nodes/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nodeBody()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Savings"));

        mockMvc.perform(delete("/api/budgets/nodes/10"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsBadRequestForPercentageValidationFailure() throws Exception {
        doThrow(new BudgetValidationException("Active budget sibling percentages must total 100%."))
            .when(budgetService)
            .createNode(eq(1), any(BudgetNodeRequest.class));

        mockMvc.perform(post("/api/budgets/profiles/1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nodeBody()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Active budget sibling percentages must total 100%."));
    }

    @Test
    void returnsNotFoundForMissingBudgetNode() throws Exception {
        doThrow(new BudgetNodeNotFoundException(99)).when(budgetService).deleteNode(99);

        mockMvc.perform(delete("/api/budgets/nodes/99"))
            .andExpect(status().isNotFound());
    }

    private String profileBody() {
        return """
            {
              "name": "Default",
              "description": "Main plan",
              "active": false
            }
            """;
    }

    private String nodeBody() {
        return """
            {
              "parentNodeId": null,
              "name": "Savings",
              "percentage": 40.00,
              "categoryId": null,
              "sortOrder": 10
            }
            """;
    }

    private BudgetProfileResponse profileResponse() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        return new BudgetProfileResponse(1, "Default", "Main plan", false, now, now);
    }

    private BudgetNodeResponse nodeResponse() {
        Instant now = Instant.parse("2026-06-18T00:00:00Z");
        return new BudgetNodeResponse(10, 1, null, "Savings", new BigDecimal("40.00"), null, 10, now, now);
    }
}
