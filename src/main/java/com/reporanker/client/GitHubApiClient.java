package com.reporanker.client;

import com.reporanker.config.GitHubApiProperties;
import com.reporanker.dto.github.EnrichedSearchResponse;
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

    /**
     * Constructs a GitHubApiClient with the given RestClient builder and properties.
     *
     * @param restClientBuilder the RestClient builder to configure
     * @param properties        the GitHub API configuration properties
     */
    public GitHubApiClient(RestClient.Builder restClientBuilder, GitHubApiProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "repo-ranker")
                .build();
    }

    /**
     * Searches GitHub repositories using the Search API.
     *
     * @param language    filter by programming language, or null for all languages
     * @param createdAfter filter by creation date, or null for no date restriction
     * @param page        the page number (1-based)
     * @param perPage     the number of results per page (max 100)
     * @return the enriched search response containing matching repositories, total count, and max values
     * @throws GitHubRateLimitExceededException if the API rate limit is exceeded
     * @throws GitHubApiException               if any other HTTP error occurs
     */
    public EnrichedSearchResponse searchRepositories(String language, Instant createdAfter, int page, int perPage) {
        String query = buildQuery(language, createdAfter);
        int maxStars = fetchMaxStars(query);
        int maxForks = fetchMaxForks(query);

        String uri = UriComponentsBuilder.fromPath("/search/repositories")
                .queryParam("q", query)
                .queryParam("sort", "stars")
                .queryParam("order", "desc")
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .build(false).toUriString();

        log.debug("Fetching GitHub repositories: {}", uri);

        GitHubSearchResponse response = null;
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
        }

        if (response == null || response.items() == null) {
            return new EnrichedSearchResponse(0, false, List.of(), maxStars, maxForks);
        }

        log.debug("Found {} repositories (total: {}, maxStars: {}, maxForks: {})",
                response.items().size(), response.totalCount(), maxStars, maxForks);
        return new EnrichedSearchResponse(response.totalCount(), response.incompleteResults(),
                response.items(), maxStars, maxForks);
    }

    private int fetchMaxStars(String query) {
        String uri = UriComponentsBuilder.fromPath("/search/repositories")
                .queryParam("q", query)
                .queryParam("sort", "stars")
                .queryParam("order", "desc")
                .queryParam("per_page", 1)
                .build(false).toUriString();
        return fetchMaxValue(uri, "maxStars");
    }

    private int fetchMaxForks(String query) {
        String uri = UriComponentsBuilder.fromPath("/search/repositories")
                .queryParam("q", query)
                .queryParam("sort", "forks")
                .queryParam("order", "desc")
                .queryParam("per_page", 1)
                .build(false).toUriString();
        return fetchMaxValue(uri, "maxForks");
    }

    private int fetchMaxValue(String uri, String label) {
        try {
            GitHubSearchResponse response = restClient.get()
                    .uri(uri)
                    .headers(h -> {
                        if (properties.token() != null && !properties.token().isBlank()) {
                            h.setBearerAuth(properties.token());
                        }
                    })
                    .retrieve()
                    .body(GitHubSearchResponse.class);

            if (response != null && response.items() != null && !response.items().isEmpty()) {
                int value = "maxStars".equals(label)
                        ? response.items().getFirst().stars()
                        : response.items().getFirst().forks();
                log.debug("Fetched {}: {}", label, value);
                return value;
            }
        } catch (RestClientResponseException ex) {
            log.warn("Failed to fetch {}: {}", label, ex.getMessage());
        }
        return 0;
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
