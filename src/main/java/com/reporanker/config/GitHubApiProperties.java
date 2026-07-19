package com.reporanker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.api")
public record GitHubApiProperties(String baseUrl, String token) {
}
