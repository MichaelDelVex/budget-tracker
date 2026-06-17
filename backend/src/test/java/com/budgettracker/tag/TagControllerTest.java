package com.budgettracker.tag;

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

@WebMvcTest(TagController.class)
@Import(RestExceptionHandler.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TagService tagService;

    @Test
    void listsTags() throws Exception {
        when(tagService.listTags()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].color").value("#336699"));
    }

    @Test
    void createsTag() throws Exception {
        when(tagService.createTag(any(TagRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/tags/1"))
            .andExpect(jsonPath("$.name").value("Tax"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "color": "blue"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists())
            .andExpect(jsonPath("$.fields.color").exists());
    }

    @Test
    void updatesTag() throws Exception {
        when(tagService.updateTag(eq(1), any(TagRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/tags/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.color").value("#336699"));
    }

    @Test
    void deletesTag() throws Exception {
        doNothing().when(tagService).deleteTag(1);

        mockMvc.perform(delete("/api/tags/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingMissingTag() throws Exception {
        doThrow(new TagNotFoundException(99)).when(tagService).deleteTag(99);

        mockMvc.perform(delete("/api/tags/99"))
            .andExpect(status().isNotFound());
    }

    private static String body() {
        return """
            {
              "name": "Tax",
              "color": "#336699"
            }
            """;
    }

    private static TagResponse response() {
        Instant now = Instant.parse("2026-06-17T00:00:00Z");
        return new TagResponse(1, "Tax", "#336699", now, now);
    }
}
