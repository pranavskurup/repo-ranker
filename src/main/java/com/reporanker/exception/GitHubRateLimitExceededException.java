package com.reporanker.exception;

import java.time.Instant;

public class GitHubRateLimitExceededException extends GitHubApiException {

    private final Instant retryAfter;

    public GitHubRateLimitExceededException(String message, Instant retryAfter) {
        super(message, 403);
        this.retryAfter = retryAfter;
    }

    public Instant getRetryAfter() {
        return retryAfter;
    }
}
