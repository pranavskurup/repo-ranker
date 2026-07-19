package com.reporanker.service;

import com.reporanker.client.GitHubApiClient;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.github.GitHubSearchResponse;
import com.reporanker.dto.response.PaginatedResponse;
import com.reporanker.dto.response.ScoredRepository;
import com.reporanker.exception.GitHubApiException;
import com.reporanker.exception.GitHubRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private RepositoryService repositoryService;

    private GitHubRepository createRepo(long id, int stars) {
        Instant now = Instant.now();
        return new GitHubRepository(id, "repo-" + id, "owner/repo-" + id,
                "https://github.com/owner/repo-" + id, stars, 10, "Java",
                now.minus(365, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS));
    }

    @Test
    void searchAndRankShouldDelegateToClientAndScorer() {
        Instant createdAfter = Instant.now().minus(30, ChronoUnit.DAYS);
        GitHubRepository repo = createRepo(1L, 100);
        ScoredRepository scored = new ScoredRepository(1L, "repo-1", "owner/repo-1",
                "https://github.com/owner/repo-1", 100, 10, "Java",
                repo.createdAt(), repo.updatedAt(), 85.5);

        GitHubSearchResponse searchResponse = new GitHubSearchResponse(1, false, List.of(repo));
        when(gitHubApiClient.searchRepositories(eq("Java"), eq(createdAfter), eq(1), eq(30)))
                .thenReturn(searchResponse);
        when(scoringService.scoreAndRank(List.of(repo)))
                .thenReturn(List.of(scored));

        PaginatedResponse<ScoredRepository> result = repositoryService.searchAndRank("Java", createdAfter, 1, 30);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().score()).isEqualTo(85.5);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        verify(gitHubApiClient).searchRepositories("Java", createdAfter, 1, 30);
        verify(scoringService).scoreAndRank(List.of(repo));
    }

    @Test
    void searchAndRankShouldReturnEmptyListWhenNoResults() {
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(0, false, List.of());
        when(gitHubApiClient.searchRepositories(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(searchResponse);
        when(scoringService.scoreAndRank(List.of()))
                .thenReturn(List.of());

        PaginatedResponse<ScoredRepository> result = repositoryService.searchAndRank(null, null, 1, 30);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    void searchAndRankShouldPassPaginationParams() {
        Instant createdAfter = Instant.now().minus(30, ChronoUnit.DAYS);
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(55, false, List.of());
        when(gitHubApiClient.searchRepositories(eq("Python"), eq(createdAfter), eq(2), eq(10)))
                .thenReturn(searchResponse);
        when(scoringService.scoreAndRank(any()))
                .thenReturn(List.of());

        PaginatedResponse<ScoredRepository> result = repositoryService.searchAndRank("Python", createdAfter, 2, 10);

        assertThat(result.currentPage()).isEqualTo(2);
        assertThat(result.perPage()).isEqualTo(10);
        assertThat(result.totalCount()).isEqualTo(55);
        assertThat(result.totalPages()).isEqualTo(6);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
        verify(gitHubApiClient).searchRepositories("Python", createdAfter, 2, 10);
    }

    @Test
    void searchAndRankShouldComputeTotalPagesCorrectly() {
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(100, false, List.of());
        when(gitHubApiClient.searchRepositories(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(searchResponse);
        when(scoringService.scoreAndRank(any()))
                .thenReturn(List.of());

        PaginatedResponse<ScoredRepository> result = repositoryService.searchAndRank(null, null, 1, 30);

        assertThat(result.totalCount()).isEqualTo(100);
        assertThat(result.totalPages()).isEqualTo(4);
    }

    @Test
    void searchAndRankShouldPropagateGitHubApiException() {
        when(gitHubApiClient.searchRepositories(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubApiException("GitHub API error: Not Found", 404));

        assertThatThrownBy(() -> repositoryService.searchAndRank(null, null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("Not Found")
                .satisfies(ex -> assertThat(((GitHubApiException) ex).getStatusCode()).isEqualTo(404));
    }

    @Test
    void searchAndRankShouldPropagateRateLimitExceeded() {
        when(gitHubApiClient.searchRepositories(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubRateLimitExceededException(
                        "rate limit exceeded", Instant.now().plusSeconds(300)));

        assertThatThrownBy(() -> repositoryService.searchAndRank(null, null, 1, 30))
                .isInstanceOf(GitHubRateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded")
                .satisfies(ex -> {
                    GitHubRateLimitExceededException rateEx = (GitHubRateLimitExceededException) ex;
                    assertThat(rateEx.getStatusCode()).isEqualTo(403);
                    assertThat(rateEx.getRetryAfter()).isAfter(Instant.now());
                });
    }

    @Test
    void searchAndRankShouldPropagateApiExceptionOn422() {
        when(gitHubApiClient.searchRepositories(eq("Java"), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubApiException("GitHub API error: Validation Failed", 422));

        assertThatThrownBy(() -> repositoryService.searchAndRank("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("Validation Failed")
                .satisfies(ex -> assertThat(((GitHubApiException) ex).getStatusCode()).isEqualTo(422));
    }

    @Test
    void searchAndRankShouldPropagateApiExceptionOn500() {
        when(gitHubApiClient.searchRepositories(isNull(), isNull(), eq(1), eq(30)))
                .thenThrow(new GitHubApiException("GitHub API error: Internal Server Error", 500));

        assertThatThrownBy(() -> repositoryService.searchAndRank(null, null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex -> assertThat(((GitHubApiException) ex).getStatusCode()).isEqualTo(500));
    }
}
