package com.reporanker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RepoRankerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepoRankerApplication.class, args);
    }
}
