package com.reporanker.controller;

import com.reporanker.dto.response.ScoredRepository;
import com.reporanker.service.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepositoryController.class)
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositoryService repositoryService;

    private ScoredRepository createScored(long id, String name, double score) {
        Instant now = Instant.now();
        return new ScoredRepository(id, name, "owner/" + name,
                "https://github.com/owner/" + name, 100, 10, "Java",
                now.minus(365, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS), score);
    }

    @Test
    void searchShouldReturnScoredRepositories() throws Exception {
        ScoredRepository repo1 = createScored(1L, "repo-1", 92.5);
        ScoredRepository repo2 = createScored(2L, "repo-2", 85.3);
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(List.of(repo1, repo2));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("repo-1"))
                .andExpect(jsonPath("$[0].score").value(92.5))
                .andExpect(jsonPath("$[1].name").value("repo-2"));
    }

    @Test
    void searchShouldPassLanguageParam() throws Exception {
        when(repositoryService.searchAndRank(eq("Java"), isNull(), eq(1), eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/repositories").param("language", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchShouldPassCreatedAfterParam() throws Exception {
        when(repositoryService.searchAndRank(isNull(), any(Instant.class), eq(1), eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/repositories")
                        .param("createdAfter", "2024-01-01T00:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test
    void searchShouldPassPaginationParams() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/repositories")
                        .param("page", "2")
                        .param("perPage", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void searchShouldReturnEmptyArrayWhenNoResults() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchShouldReturnAllRepositoryFields() throws Exception {
        ScoredRepository repo = createScored(1L, "test-repo", 78.9);
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(List.of(repo));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("test-repo"))
                .andExpect(jsonPath("$[0].full_name").value("owner/test-repo"))
                .andExpect(jsonPath("$[0].html_url").value("https://github.com/owner/test-repo"))
                .andExpect(jsonPath("$[0].stars").value(100))
                .andExpect(jsonPath("$[0].forks").value(10))
                .andExpect(jsonPath("$[0].language").value("Java"))
                .andExpect(jsonPath("$[0].created_at").exists())
                .andExpect(jsonPath("$[0].updated_at").exists())
                .andExpect(jsonPath("$[0].score").value(78.9));
    }

    @Test
    void searchShouldReturn400ForInvalidCreatedAfterFormat() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                        .param("createdAfter", "invalid-date"))
                .andExpect(status().isBadRequest());
    }
}
