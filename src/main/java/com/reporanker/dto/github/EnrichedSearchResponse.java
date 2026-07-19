package com.reporanker.dto.github;

import java.util.List;

/**
 * Enriched search response containing the GitHub API results along with
 * computed maximum stars and forks across the returned items.
 *
 * @param totalCount         the total number of matching repositories
 * @param incompleteResults  whether the results may be incomplete
 * @param items              the list of matching repositories
 * @param maxStars           the maximum stars across all items
 * @param maxForks           the maximum forks across all items
 */
public record EnrichedSearchResponse(
        int totalCount,
        boolean incompleteResults,
        List<GitHubRepository> items,
        int maxStars,
        int maxForks
) {
    /**
     * Creates an enriched response by computing max stars and forks from the given search response.
     * Returns an empty response with zero max values if the input is null or contains no items.
     *
     * @param response the GitHub API search response, or null
     * @return an enriched search response with computed max values
     */
    public static EnrichedSearchResponse from(GitHubSearchResponse response) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return new EnrichedSearchResponse(0, false, List.of(), 0, 0);
        }
        int maxStars = response.items().stream().mapToInt(GitHubRepository::stars).max().orElse(0);
        int maxForks = response.items().stream().mapToInt(GitHubRepository::forks).max().orElse(0);
        return new EnrichedSearchResponse(response.totalCount(), response.incompleteResults(),
                response.items(), maxStars, maxForks);
    }
}
