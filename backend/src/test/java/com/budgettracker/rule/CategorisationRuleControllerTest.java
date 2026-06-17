package com.budgettracker.rule;

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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategorisationRuleController.class)
@Import(RestExceptionHandler.class)
class CategorisationRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategorisationRuleService ruleService;

    @Test
    void listsRules() throws Exception {
        when(ruleService.listRules()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/categorisation-rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].matchText").value("coffee"));
    }

    @Test
    void createsRule() throws Exception {
        when(ruleService.createRule(any(CategorisationRuleRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/categorisation-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/categorisation-rules/1"))
            .andExpect(jsonPath("$.priority").value(5));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/categorisation-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "matchText": "",
                      "categoryId": 0,
                      "tagId": 0,
                      "active": true,
                      "priority": -1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.matchText").exists())
            .andExpect(jsonPath("$.fields.categoryId").exists())
            .andExpect(jsonPath("$.fields.tagId").exists())
            .andExpect(jsonPath("$.fields.priority").exists());
    }

    @Test
    void updatesRule() throws Exception {
        when(ruleService.updateRule(eq(1), any(CategorisationRuleRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/categorisation-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matchText").value("coffee"));
    }

    @Test
    void deletesRule() throws Exception {
        doNothing().when(ruleService).deleteRule(1);

        mockMvc.perform(delete("/api/categorisation-rules/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingMissingRule() throws Exception {
        doThrow(new CategorisationRuleNotFoundException(99)).when(ruleService).deleteRule(99);

        mockMvc.perform(delete("/api/categorisation-rules/99"))
            .andExpect(status().isNotFound());
    }

    private static String body() {
        return """
            {
              "matchText": "coffee",
              "categoryId": 2,
              "tagId": 1,
              "active": true,
              "priority": 5
            }
            """;
    }

    private static CategorisationRuleResponse response() {
        Instant now = Instant.parse("2026-06-17T00:00:00Z");
        return new CategorisationRuleResponse(1, "coffee", 2, 1, true, 5, now, now);
    }
}
