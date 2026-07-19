package com.reporanker.service;

import com.reporanker.client.GitHubApiClient;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.github.GitHubSearchResponse;
import com.reporanker.dto.response.PaginatedResponse;
import com.reporanker.dto.response.ScoredRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the repository search pipeline: fetches repositories from GitHub,
 * applies scoring via {@link ScoringService}, and returns paginated results.
 */
@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    private final GitHubApiClient gitHubApiClient;
    private final ScoringService scoringService;

    public RepositoryService(GitHubApiClient gitHubApiClient, ScoringService scoringService) {
        this.gitHubApiClient = gitHubApiClient;
        this.scoringService = scoringService;
    }

    public PaginatedResponse<ScoredRepository> searchAndRank(String language, Instant createdAfter, int page, int perPage) {
        log.debug("Searching repositories: language={}, createdAfter={}, page={}, perPage={}",
                language, createdAfter, page, perPage);

        GitHubSearchResponse searchResponse = gitHubApiClient.searchRepositories(language, createdAfter, page, perPage);
        List<ScoredRepository> scored = scoringService.scoreAndRank(searchResponse.items());
        return PaginatedResponse.of(scored, searchResponse.totalCount(), page, perPage);
    }
}
