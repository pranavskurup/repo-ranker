package com.reporanker;

import com.reporanker.config.GitHubApiProperties;
import com.reporanker.config.ScoringProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the Repo Ranker application.
 * Scores and ranks GitHub repositories based on popularity metrics
 * including stars, forks, and recency of activity.
 */
@SpringBootApplication
@EnableConfigurationProperties({GitHubApiProperties.class, ScoringProperties.class})
public class RepoRankerApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RepoRankerApplication.class, args);
    }
}
