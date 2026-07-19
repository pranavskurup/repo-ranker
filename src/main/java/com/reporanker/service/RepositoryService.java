package com.reporanker.service;

import com.reporanker.client.GitHubApiClient;
import com.reporanker.dto.github.EnrichedSearchResponse;
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

    /**
     * Constructs a RepositoryService with the given dependencies.
     *
     * @param gitHubApiClient the client for fetching repositories from GitHub
     * @param scoringService  the service for computing repository scores
     */
    public RepositoryService(GitHubApiClient gitHubApiClient, ScoringService scoringService) {
        this.gitHubApiClient = gitHubApiClient;
        this.scoringService = scoringService;
    }

    /**
     * Searches GitHub repositories and returns scored, paginated results.
     *
     * @param language    filter by programming language, or null for all languages
     * @param createdAfter filter by creation date, or null for no date restriction
     * @param page        the page number (1-based)
     * @param perPage     the number of results per page
     * @return paginated response containing scored repositories and metadata
     */
    public PaginatedResponse<ScoredRepository> searchAndRank(String language, Instant createdAfter, int page, int perPage) {
        log.debug("Searching repositories: language={}, createdAfter={}, page={}, perPage={}",
                language, createdAfter, page, perPage);

        EnrichedSearchResponse searchResponse = gitHubApiClient.searchRepositories(language, createdAfter, page, perPage);
        List<ScoredRepository> scored = scoringService.scoreAndRank(searchResponse.items(),
                searchResponse.maxStars(), searchResponse.maxForks());
        return PaginatedResponse.of(scored, searchResponse.totalCount(), page, perPage);
    }
}
