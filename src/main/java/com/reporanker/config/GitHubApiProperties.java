package com.reporanker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the GitHub API client.
 *
 * @param baseUrl the GitHub API base URL (e.g., https://api.github.com)
 * @param token   the personal access token for authenticated requests (optional)
 */
@ConfigurationProperties(prefix = "github.api")
public record GitHubApiProperties(String baseUrl, String token) {
}
