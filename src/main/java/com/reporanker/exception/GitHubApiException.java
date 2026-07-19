package com.reporanker.exception;

/**
 * Runtime exception thrown when the GitHub API returns an HTTP error response.
 * Carries the original HTTP status code for mapping to appropriate client responses.
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
