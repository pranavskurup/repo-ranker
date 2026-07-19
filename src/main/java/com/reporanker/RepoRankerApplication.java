package com.reporanker;

import com.reporanker.config.GitHubApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the Repo Ranker application.
 * Scores and ranks GitHub repositories based on popularity metrics
 * including stars, forks, and recency of activity.
 */
@SpringBootApplication
@EnableConfigurationProperties(GitHubApiProperties.class)
public class RepoRankerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepoRankerApplication.class, args);
    }
}
