package com.reporanker.exception;

import java.time.Instant;

/**
 * Exception thrown when the GitHub API rate limit has been exceeded (HTTP 403).
 * Contains the timestamp after which the client should retry.
 */
public class GitHubRateLimitExceededException extends GitHubApiException {

    private final Instant retryAfter;

    /**
     * Creates an exception with the given message and retry-after timestamp.
     *
     * @param message     the error message
     * @param retryAfter  the timestamp after which to retry
     */
    public GitHubRateLimitExceededException(String message, Instant retryAfter) {
        super(message, 403);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the timestamp after which the client should retry the request.
     *
     * @return the retry-after timestamp
     */
    public Instant getRetryAfter() {
        return retryAfter;
    }
}
