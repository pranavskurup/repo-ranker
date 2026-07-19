package com.reporanker.exception;

/**
 * Runtime exception thrown when the GitHub API returns an HTTP error response.
 * Carries the original HTTP status code for mapping to appropriate client responses.
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    /**
     * Creates an exception with the given message and HTTP status code.
     *
     * @param message    the error message
     * @param statusCode the HTTP status code from the GitHub API
     */
    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Creates an exception with the given message, status code, and cause.
     *
     * @param message    the error message
     * @param statusCode the HTTP status code from the GitHub API
     * @param cause      the underlying cause
     */
    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code from the GitHub API response.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
