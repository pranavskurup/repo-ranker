package com.reporanker.controller;

import com.reporanker.dto.response.PaginatedResponse;
import com.reporanker.dto.response.ScoredRepository;
import com.reporanker.exception.GitHubApiException;
import com.reporanker.exception.GitHubRateLimitExceededException;
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

    private PaginatedResponse<ScoredRepository> paginated(List<ScoredRepository> items, int totalCount, int page, int perPage) {
        return PaginatedResponse.of(items, totalCount, page, perPage);
    }

    @Test
    void searchShouldReturnScoredRepositories() throws Exception {
        ScoredRepository repo1 = createScored(1L, "repo-1", 92.5);
        ScoredRepository repo2 = createScored(2L, "repo-2", 85.3);
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(paginated(List.of(repo1, repo2), 2, 1, 30));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("repo-1"))
                .andExpect(jsonPath("$.items[0].score").value(92.5))
                .andExpect(jsonPath("$.items[1].name").value("repo-2"))
                .andExpect(jsonPath("$.total_count").value(2))
                .andExpect(jsonPath("$.current_page").value(1))
                .andExpect(jsonPath("$.per_page").value(30))
                .andExpect(jsonPath("$.total_pages").value(1))
                .andExpect(jsonPath("$.has_next").value(false))
                .andExpect(jsonPath("$.has_previous").value(false));
    }

    @Test
    void searchShouldPassLanguageParam() throws Exception {
        when(repositoryService.searchAndRank(eq("Java"), isNull(), eq(1), eq(30)))
                .thenReturn(paginated(List.of(), 0, 1, 30));

        mockMvc.perform(get("/api/v1/repositories").param("language", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total_count").value(0));
    }

    @Test
    void searchShouldPassCreatedAfterParam() throws Exception {
        when(repositoryService.searchAndRank(isNull(), any(Instant.class), eq(1), eq(30)))
                .thenReturn(paginated(List.of(), 0, 1, 30));

        mockMvc.perform(get("/api/v1/repositories")
                        .param("createdAfter", "2024-01-01T00:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test
    void searchShouldPassPaginationParams() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(paginated(List.of(), 0, 2, 10));

        mockMvc.perform(get("/api/v1/repositories")
                        .param("page", "2")
                        .param("perPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_page").value(2))
                .andExpect(jsonPath("$.per_page").value(10));
    }

    @Test
    void searchShouldReturnEmptyArrayWhenNoResults() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(paginated(List.of(), 0, 1, 30));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total_count").value(0));
    }

    @Test
    void searchShouldReturnAllRepositoryFields() throws Exception {
        ScoredRepository repo = createScored(1L, "test-repo", 78.9);
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(paginated(List.of(repo), 1, 1, 30));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("test-repo"))
                .andExpect(jsonPath("$.items[0].full_name").value("owner/test-repo"))
                .andExpect(jsonPath("$.items[0].html_url").value("https://github.com/owner/test-repo"))
                .andExpect(jsonPath("$.items[0].stars").value(100))
                .andExpect(jsonPath("$.items[0].forks").value(10))
                .andExpect(jsonPath("$.items[0].language").value("Java"))
                .andExpect(jsonPath("$.items[0].created_at").exists())
                .andExpect(jsonPath("$.items[0].updated_at").exists())
                .andExpect(jsonPath("$.items[0].score").value(78.9));
    }

    @Test
    void searchShouldReturn400ForInvalidCreatedAfterFormat() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                        .param("createdAfter", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchShouldReturnPaginationMetadataForMultiplePages() throws Exception {
        ScoredRepository repo = createScored(1L, "repo-1", 90.0);
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(paginated(List.of(repo), 55, 2, 10));

        mockMvc.perform(get("/api/v1/repositories")
                        .param("page", "2")
                        .param("perPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_count").value(55))
                .andExpect(jsonPath("$.current_page").value(2))
                .andExpect(jsonPath("$.per_page").value(10))
                .andExpect(jsonPath("$.total_pages").value(6))
                .andExpect(jsonPath("$.has_next").value(true))
                .andExpect(jsonPath("$.has_previous").value(true));
    }

    @Test
    void searchShouldReturn502WhenGitHubApiReturns404() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubApiException("GitHub API error: Not Found", 404));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("github_api_error"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void searchShouldReturn429WhenRateLimitExceeded() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubRateLimitExceededException(
                        "rate limit exceeded", Instant.now().plusSeconds(300)));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.retry_after").exists())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void searchShouldReturn502WhenGitHubApiReturns500() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubApiException("GitHub API error: Internal Server Error", 500));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("github_api_error"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void searchShouldReturn422WhenGitHubApiReturns422() throws Exception {
        when(repositoryService.searchAndRank(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubApiException("GitHub API error: Validation Failed", 422));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("github_api_error"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void searchShouldReturn400ForInvalidPageParam() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                        .param("page", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchShouldReturn400ForInvalidPerPageParam() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                        .param("perPage", "abc"))
                .andExpect(status().isBadRequest());
    }
}
