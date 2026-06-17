package com.budgettracker.category;

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

import com.budgettracker.domain.category.CategoryType;
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

@WebMvcTest(CategoryController.class)
@Import(RestExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void listsCategories() throws Exception {
        when(categoryService.listCategories()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Groceries"));
    }

    @Test
    void createsCategory() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/categories/1"))
            .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "type": null,
                      "defaultCategory": false,
                      "active": true,
                      "sortOrder": -1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists())
            .andExpect(jsonPath("$.fields.type").exists())
            .andExpect(jsonPath("$.fields.sortOrder").exists());
    }

    @Test
    void updatesCategory() throws Exception {
        when(categoryService.updateCategory(eq(1), any(CategoryRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Groceries"));
    }

    @Test
    void deletesCategory() throws Exception {
        doNothing().when(categoryService).deleteCategory(1);

        mockMvc.perform(delete("/api/categories/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingMissingCategory() throws Exception {
        doThrow(new CategoryNotFoundException(99)).when(categoryService).deleteCategory(99);

        mockMvc.perform(delete("/api/categories/99"))
            .andExpect(status().isNotFound());
    }

    private static String body() {
        return """
            {
              "name": "Groceries",
              "type": "EXPENSE",
              "defaultCategory": true,
              "active": true,
              "sortOrder": 10
            }
            """;
    }

    private static CategoryResponse response() {
        Instant now = Instant.parse("2026-06-17T00:00:00Z");
        return new CategoryResponse(1, "Groceries", CategoryType.EXPENSE, true, true, 10, now, now);
    }
}
