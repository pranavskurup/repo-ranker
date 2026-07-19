package com.reporanker.service;

import com.reporanker.client.GitHubApiClient;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.response.ScoredRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    private final GitHubApiClient gitHubApiClient;
    private final ScoringService scoringService;

    public RepositoryService(GitHubApiClient gitHubApiClient, ScoringService scoringService) {
        this.gitHubApiClient = gitHubApiClient;
        this.scoringService = scoringService;
    }

    public List<ScoredRepository> searchAndRank(String language, Instant createdAfter, int page, int perPage) {
        log.debug("Searching repositories: language={}, createdAfter={}, page={}, perPage={}",
                language, createdAfter, page, perPage);

        List<GitHubRepository> repositories = gitHubApiClient.searchRepositories(language, createdAfter, page, perPage);
        return scoringService.scoreAndRank(repositories);
    }
}
