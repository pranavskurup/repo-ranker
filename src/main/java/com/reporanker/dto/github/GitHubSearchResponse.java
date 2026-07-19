package com.reporanker.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for the GitHub Search API response.
 *
 * @param totalCount         the total number of matching repositories
 * @param incompleteResults  whether the results may be incomplete
 * @param items              the list of matching repositories
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubSearchResponse(
        @JsonProperty("total_count") int totalCount,
        @JsonProperty("incomplete_results") boolean incompleteResults,
        List<GitHubRepository> items
) {
}
