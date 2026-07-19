package com.reporanker.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

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
