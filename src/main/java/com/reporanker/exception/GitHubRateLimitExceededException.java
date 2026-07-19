package com.reporanker.exception;

import java.time.Instant;

/**
 * Exception thrown when the GitHub API rate limit has been exceeded (HTTP 403).
 * Contains the timestamp after which the client should retry.
 */
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
