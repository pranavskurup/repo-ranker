package com.reporanker.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response DTO representing a GitHub repository with a computed popularity score.
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
 * @param score     the computed popularity score (0-100)
 */
public record ScoredRepository(
        long id,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String url,
        int stars,
        int forks,
        String language,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        double score
) {
}
