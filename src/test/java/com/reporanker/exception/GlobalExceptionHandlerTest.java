package com.reporanker.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRateLimitExceededShouldReturn429() {
        Instant retryAfter = Instant.now().plus(120, ChronoUnit.SECONDS);
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("rate limited", retryAfter);

        ResponseEntity<Map<String, Object>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("error", "rate_limit_exceeded");
        assertThat(response.getBody()).containsEntry("status", 429);
        assertThat(response.getBody()).containsEntry("retry_after", retryAfter.toString());
    }

    @Test
    void handleRateLimitExceededShouldIncludeRetryAfterHeader() {
        Instant retryAfter = Instant.now().plus(60, ChronoUnit.SECONDS);
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("rate limited", retryAfter);

        ResponseEntity<Map<String, Object>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getHeaders().getFirst("Retry-After")).isNotNull();
    }

    @Test
    void handleGitHubApiExceptionShouldReturnOriginalStatusCode() {
        GitHubApiException ex = new GitHubApiException("Not Found", 404);

        ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "github_api_error");
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("message", "Not Found");
    }

    @Test
    void handleGitHubApiExceptionShouldFallBackTo502ForUnknownStatus() {
        GitHubApiException ex = new GitHubApiException("Unknown error", 999);

        ResponseEntity<Map<String, Object>> response = handler.handleGitHubApiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("status", 502);
    }

    @Test
    void handleMissingParamShouldReturn400() {
        MissingServletRequestParameterException ex = mock(MissingServletRequestParameterException.class);
        when(ex.getParameterName()).thenReturn("language");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "bad_request");
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Missing required parameter: language");
    }

    @Test
    void handleTypeMismatchShouldReturn400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("page");

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "bad_request");
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Invalid value for parameter: page");
    }

    @Test
    void handleGenericExceptionShouldReturn500() {
        Exception ex = new RuntimeException("something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "internal_error");
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
    }
}
