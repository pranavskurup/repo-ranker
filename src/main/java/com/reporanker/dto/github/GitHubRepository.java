package com.reporanker.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO representing a GitHub repository from the Search API.
 *
 * @param id        the repository ID
 * @param name      the repository name
 * @param fullName  the full name (owner/name)
 * @param url       the HTML URL of the repository
 * @param stars     the number of stargazers
 * @param forks     the number of forks
 * @param language  the primary programming language
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepository(
        long id,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String url,
        @JsonProperty("stargazers_count") int stars,
        @JsonProperty("forks_count") int forks,
        String language,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
