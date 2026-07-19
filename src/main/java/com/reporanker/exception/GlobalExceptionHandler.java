package com.reporanker.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler translating application exceptions into
 * structured JSON error responses with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles GitHub API rate limit exceeded errors (HTTP 429).
     *
     * @param ex the rate limit exception
     * @return error response with retry-after information
     */
    @ExceptionHandler(GitHubRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(GitHubRateLimitExceededException ex) {
        log.warn("GitHub API rate limit exceeded: {}", ex.getMessage());
        Map<String, Object> body = Map.of(
                "error", "rate_limit_exceeded",
                "message", ex.getMessage(),
                "status", 429,
                "retry_after", ex.getRetryAfter().toString()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfter().getEpochSecond() - Instant.now().getEpochSecond()))
                .body(body);
    }

    /**
     * Handles general GitHub API errors with the original HTTP status code.
     *
     * @param ex the API exception
     * @return error response with the original status code
     */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiException(GitHubApiException ex) {
        log.error("GitHub API error (status {}): {}", ex.getStatusCode(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        Map<String, Object> body = Map.of(
                "error", "github_api_error",
                "message", ex.getMessage(),
                "status", status.value()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles missing request parameter errors (HTTP 400).
     *
     * @param ex the missing parameter exception
     * @return bad request error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, Object> body = Map.of(
                "error", "bad_request",
                "message", "Missing required parameter: " + ex.getParameterName(),
                "status", 400
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles type mismatch errors for request parameters (HTTP 400).
     *
     * @param ex the type mismatch exception
     * @return bad request error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = Map.of(
                "error", "bad_request",
                "message", "Invalid value for parameter: " + ex.getName(),
                "status", 400
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles all other unexpected exceptions (HTTP 500).
     *
     * @param ex the unexpected exception
     * @return internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        Map<String, Object> body = Map.of(
                "error", "internal_error",
                "message", "An unexpected error occurred",
                "status", 500
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
