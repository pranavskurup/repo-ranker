package com.reporanker.service;

import com.reporanker.client.GitHubApiClient;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.response.ScoredRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        when(gitHubApiClient.searchRepositories(eq("Java"), eq(createdAfter), eq(1), eq(30)))
                .thenReturn(List.of(repo));
        when(scoringService.scoreAndRank(List.of(repo)))
                .thenReturn(List.of(scored));

        List<ScoredRepository> result = repositoryService.searchAndRank("Java", createdAfter, 1, 30);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().score()).isEqualTo(85.5);
        verify(gitHubApiClient).searchRepositories("Java", createdAfter, 1, 30);
        verify(scoringService).scoreAndRank(List.of(repo));
    }

    @Test
    void searchAndRankShouldReturnEmptyListWhenNoResults() {
        when(gitHubApiClient.searchRepositories(isNull(), isNull(), eq(1), eq(30)))
                .thenReturn(List.of());
        when(scoringService.scoreAndRank(List.of()))
                .thenReturn(List.of());

        List<ScoredRepository> result = repositoryService.searchAndRank(null, null, 1, 30);

        assertThat(result).isEmpty();
    }

    @Test
    void searchAndRankShouldPassPaginationParams() {
        Instant createdAfter = Instant.now().minus(30, ChronoUnit.DAYS);
        when(gitHubApiClient.searchRepositories(eq("Python"), eq(createdAfter), eq(2), eq(10)))
                .thenReturn(List.of());
        when(scoringService.scoreAndRank(any()))
                .thenReturn(List.of());

        repositoryService.searchAndRank("Python", createdAfter, 2, 10);

        verify(gitHubApiClient).searchRepositories("Python", createdAfter, 2, 10);
    }
}
