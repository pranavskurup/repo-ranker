package com.reporanker.client;

import com.reporanker.config.GitHubApiProperties;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.github.GitHubSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    private final RestClient restClient;
    private final GitHubApiProperties properties;

    public GitHubApiClient(RestClient.Builder restClientBuilder, GitHubApiProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "repo-ranker")
                .build();
    }

    public List<GitHubRepository> searchRepositories(String language, Instant createdAfter, int page, int perPage) {
        String query = buildQuery(language, createdAfter);
        String uri = UriComponentsBuilder.fromPath("/search/repositories")
                .queryParam("q", query)
                .queryParam("sort", "stars")
                .queryParam("order", "desc")
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .build(false).toUriString();

        log.debug("Fetching GitHub repositories: {}", uri);

        GitHubSearchResponse response = restClient.get()
                .uri(uri)
                .headers(h -> {
                    if (properties.token() != null && !properties.token().isBlank()) {
                        h.setBearerAuth(properties.token());
                    }
                })
                .retrieve()
                .body(GitHubSearchResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }

        log.debug("Found {} repositories (total: {})", response.items().size(), response.totalCount());
        return response.items();
    }

    private String buildQuery(String language, Instant createdAfter) {
        StringBuilder query = new StringBuilder();
        if (language != null && !language.isBlank()) {
            query.append("language:").append(language);
        }
        if (createdAfter != null) {
            if (!query.isEmpty()) query.append('+');
            query.append("created:>").append(createdAfter.toString(), 0, 10);
        }
        if (query.isEmpty()) {
            query.append("stars:>0");
        }
        return query.toString();
    }
}
