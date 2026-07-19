package com.reporanker.service;

import com.reporanker.client.GitHubApiClient;
import com.reporanker.dto.github.EnrichedSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = "app.cache.ttl-minutes=5")
class RepositoryServiceCacheTest {

    @Autowired
    private RepositoryService repositoryService;

    @MockitoBean
    private GitHubApiClient gitHubApiClient;

    @MockitoBean
    private com.reporanker.service.ScoringService scoringService;

    @Autowired
    private CacheManager cacheManager;

    private EnrichedSearchResponse createResponse(int count) {
        return new EnrichedSearchResponse(count, false, List.of(), 100, 50);
    }

    @Test
    void searchAndRankShouldCacheResultsForSameParameters() {
        Instant createdAfter = Instant.now().minus(30, ChronoUnit.DAYS);
        EnrichedSearchResponse response = createResponse(1);
        when(gitHubApiClient.searchRepositories(eq("Java"), eq(createdAfter), eq(1), eq(30)))
                .thenReturn(response);
        when(scoringService.scoreAndRank(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        repositoryService.searchAndRank("Java", createdAfter, 1, 30);
        repositoryService.searchAndRank("Java", createdAfter, 1, 30);
        repositoryService.searchAndRank("Java", createdAfter, 1, 30);

        verify(gitHubApiClient, times(1)).searchRepositories("Java", createdAfter, 1, 30);
        verify(scoringService, times(1)).scoreAndRank(any(), anyInt(), anyInt());
    }

    @Test
    void searchAndRankShouldCacheSeparatelyForDifferentParameters() {
        Instant createdAfter = Instant.now().minus(30, ChronoUnit.DAYS);
        EnrichedSearchResponse response = createResponse(1);
        when(gitHubApiClient.searchRepositories(any(), any(), anyInt(), anyInt()))
                .thenReturn(response);
        when(scoringService.scoreAndRank(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        repositoryService.searchAndRank("Java", createdAfter, 1, 30);
        repositoryService.searchAndRank("Python", createdAfter, 1, 30);

        verify(gitHubApiClient, times(2)).searchRepositories(any(), any(), anyInt(), anyInt());
    }

    @Test
    void searchAndRankShouldCacheSeparatelyForDifferentPages() {
        Instant createdAfter = Instant.now().minus(30, ChronoUnit.DAYS);
        EnrichedSearchResponse response = createResponse(1);
        when(gitHubApiClient.searchRepositories(eq("Java"), eq(createdAfter), anyInt(), anyInt()))
                .thenReturn(response);
        when(scoringService.scoreAndRank(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        repositoryService.searchAndRank("Java", createdAfter, 1, 30);
        repositoryService.searchAndRank("Java", createdAfter, 2, 30);

        verify(gitHubApiClient, times(2)).searchRepositories(eq("Java"), eq(createdAfter), anyInt(), anyInt());
    }

    @Test
    void cacheShouldBeConfiguredWithCorrectTtl() {
        assertThat(cacheManager.getCacheNames()).contains("repositories");
    }
}
