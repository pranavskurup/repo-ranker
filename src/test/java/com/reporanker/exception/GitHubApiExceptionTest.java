package com.reporanker.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubApiExceptionTest {

    @Test
    void constructorShouldStoreMessageAndStatusCode() {
        GitHubApiException ex = new GitHubApiException("Not Found", 404);

        assertThat(ex.getMessage()).isEqualTo("Not Found");
        assertThat(ex.getStatusCode()).isEqualTo(404);
    }

    @Test
    void constructorWithCauseShouldStoreAllFields() {
        RuntimeException cause = new RuntimeException("connection refused");
        GitHubApiException ex = new GitHubApiException("API error", 502, cause);

        assertThat(ex.getMessage()).isEqualTo("API error");
        assertThat(ex.getStatusCode()).isEqualTo(502);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shouldExtendRuntimeException() {
        GitHubApiException ex = new GitHubApiException("error", 500);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getStatusCodeShouldReturnCorrectValue() {
        GitHubApiException ex = new GitHubApiException("Rate limited", 403);

        assertThat(ex.getStatusCode()).isEqualTo(403);
    }
}
