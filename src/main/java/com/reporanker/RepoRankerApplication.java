package com.reporanker;

import com.reporanker.config.GitHubApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GitHubApiProperties.class)
public class RepoRankerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepoRankerApplication.class, args);
    }
}
