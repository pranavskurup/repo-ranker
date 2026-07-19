package com.reporanker.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRateLimitExceededExceptionTest {

    @Test
    void constructorShouldSetStatusCodeTo403() {
        Instant retryAfter = Instant.now().plus(60, ChronoUnit.SECONDS);
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("rate limited", retryAfter);

        assertThat(ex.getStatusCode()).isEqualTo(403);
    }

    @Test
    void constructorShouldStoreRetryAfterTimestamp() {
        Instant retryAfter = Instant.parse("2026-07-20T00:00:00Z");
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("rate limited", retryAfter);

        assertThat(ex.getRetryAfter()).isEqualTo(retryAfter);
    }

    @Test
    void constructorShouldStoreMessage() {
        Instant retryAfter = Instant.now();
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("API rate limit exceeded", retryAfter);

        assertThat(ex.getMessage()).isEqualTo("API rate limit exceeded");
    }

    @Test
    void shouldExtendGitHubApiException() {
        Instant retryAfter = Instant.now();
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("rate limited", retryAfter);

        assertThat(ex).isInstanceOf(GitHubApiException.class);
    }

    @Test
    void getRetryAfterShouldReturnCorrectTimestamp() {
        Instant expected = Instant.now().plus(300, ChronoUnit.SECONDS);
        GitHubRateLimitExceededException ex = new GitHubRateLimitExceededException("rate limited", expected);

        assertThat(ex.getRetryAfter()).isEqualTo(expected);
        assertThat(ex.getRetryAfter()).isAfter(Instant.now());
    }
}
