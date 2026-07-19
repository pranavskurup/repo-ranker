package com.reporanker.client;

import com.reporanker.config.GitHubApiProperties;
import com.reporanker.dto.github.GitHubSearchResponse;
import com.reporanker.exception.GitHubApiException;
import com.reporanker.exception.GitHubRateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Client for the GitHub Search API. Fetches repositories with optional
 * language and date filters, sorted by stars in descending order.
 * Handles HTTP errors and rate limiting with custom exceptions.
 */
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

    public GitHubSearchResponse searchRepositories(String language, Instant createdAfter, int page, int perPage) {
        String query = buildQuery(language, createdAfter);
        String uri = UriComponentsBuilder.fromPath("/search/repositories")
                .queryParam("q", query)
                .queryParam("sort", "stars")
                .queryParam("order", "desc")
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .build(false).toUriString();

        log.debug("Fetching GitHub repositories: {}", uri);

        GitHubSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .headers(h -> {
                        if (properties.token() != null && !properties.token().isBlank()) {
                            h.setBearerAuth(properties.token());
                        }
                    })
                    .retrieve()
                    .body(GitHubSearchResponse.class);
        } catch (RestClientResponseException ex) {
            handleHttpError(ex);
            return new GitHubSearchResponse(0, false, List.of());
        }

        if (response == null) {
            return new GitHubSearchResponse(0, false, List.of());
        }

        if (response.items() == null) {
            return new GitHubSearchResponse(response.totalCount(), false, List.of());
        }

        log.debug("Found {} repositories (total: {})", response.items().size(), response.totalCount());
        return response;
    }

    private void handleHttpError(RestClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        String errorMessage = extractErrorMessage(ex.getResponseBodyAsString());

        log.error("GitHub API returned status {}: {}", statusCode, errorMessage);

        if (statusCode == 403 && isRateLimitExceeded(ex)) {
            Instant retryAfter = parseRetryAfter(ex.getResponseHeaders() != null
                    ? ex.getResponseHeaders().getFirst("X-RateLimit-Reset") : null);
            throw new GitHubRateLimitExceededException(
                    "GitHub API rate limit exceeded. " + errorMessage, retryAfter);
        }

        throw new GitHubApiException("GitHub API error: " + errorMessage, statusCode);
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Unknown GitHub API error";
        }
        int messageStart = responseBody.indexOf("\"message\":\"");
        if (messageStart == -1) {
            return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
        }
        int valueStart = messageStart + "\"message\":\"".length();
        int valueEnd = responseBody.indexOf("\"", valueStart);
        if (valueEnd == -1) {
            return responseBody.substring(valueStart);
        }
        return responseBody.substring(valueStart, valueEnd);
    }

    private boolean isRateLimitExceeded(RestClientResponseException ex) {
        try {
            String remaining = ex.getResponseHeaders() != null
                    ? ex.getResponseHeaders().getFirst("X-RateLimit-Remaining") : null;
            return "0".equals(remaining);
        } catch (Exception e) {
            return false;
        }
    }

    private Instant parseRetryAfter(String resetTimestamp) {
        try {
            if (resetTimestamp != null) {
                long epochSecond = Long.parseLong(resetTimestamp);
                return Instant.ofEpochSecond(epochSecond);
            }
        } catch (NumberFormatException e) {
            log.debug("Failed to parse X-RateLimit-Reset header: {}", resetTimestamp);
        }
        return Instant.now().plusSeconds(60);
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
