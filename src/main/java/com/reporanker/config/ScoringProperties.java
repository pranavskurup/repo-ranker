package com.reporanker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the repository scoring algorithm.
 *
 * @param maxStars the maximum stars value used for log-normalization (default: 100,000)
 * @param maxForks the maximum forks value used for log-normalization (default: 10,000)
 */
@ConfigurationProperties(prefix = "app.scoring")
public record ScoringProperties(int maxStars, int maxForks) {
    public ScoringProperties {
        if (maxStars <= 0) maxStars = 100_000;
        if (maxForks <= 0) maxForks = 10_000;
    }
}
