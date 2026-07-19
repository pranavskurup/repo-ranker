package com.reporanker.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

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
