package com.reporanker.client;

import com.reporanker.config.GitHubApiProperties;
import com.reporanker.dto.github.EnrichedSearchResponse;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.github.GitHubSearchResponse;
import com.reporanker.exception.GitHubApiException;
import com.reporanker.exception.GitHubRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GitHubApiClientTest {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.RequestHeadersSpec headersSpec;
    private RestClient.ResponseSpec responseSpec;
    private GitHubApiClient gitHubApiClient;

    private static final GitHubSearchResponse EMPTY_RESPONSE = new GitHubSearchResponse(0, false, List.of());

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        GitHubApiProperties properties = new GitHubApiProperties("https://api.github.com", "test-token");

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.baseUrl((String) any())).thenReturn(builder);
        when(builder.defaultHeader(any(), any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        gitHubApiClient = new GitHubApiClient(builder, properties);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(headersSpec);
        when(headersSpec.headers(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(EMPTY_RESPONSE);
    }

    @Test
    void searchRepositoriesShouldReturnResults() {
        GitHubRepository repo = new GitHubRepository(1L, "test-repo", "owner/test-repo",
                "https://github.com/owner/test-repo", 100, 20, "Java",
                Instant.parse("2024-01-15T10:30:00Z"), Instant.parse("2024-06-01T14:20:00Z"));
        GitHubSearchResponse apiResponse = new GitHubSearchResponse(1, false, List.of(repo));
        when(responseSpec.body(GitHubSearchResponse.class))
                .thenReturn(apiResponse)
                .thenReturn(apiResponse)
                .thenReturn(apiResponse);

        EnrichedSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().name()).isEqualTo("test-repo");
        assertThat(result.items().getFirst().stars()).isEqualTo(100);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.maxStars()).isEqualTo(100);
        assertThat(result.maxForks()).isEqualTo(20);
    }

    @Test
    void searchRepositoriesShouldReturnEmptyListWhenResponseIsNull() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(null);

        EnrichedSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    @Test
    void searchRepositoriesShouldReturnEmptyListWhenItemsIsNull() {
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(0, false, null);
        when(responseSpec.body(GitHubSearchResponse.class))
                .thenReturn(EMPTY_RESPONSE)
                .thenReturn(EMPTY_RESPONSE)
                .thenReturn(searchResponse);

        EnrichedSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void searchRepositoriesShouldBuildQueryWithLanguageOnly() {
        gitHubApiClient.searchRepositories("Java", null, 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        for (String uri : allUris) {
            assertThat(uri).contains("q=language:Java");
            assertThat(uri).doesNotContain("created");
        }
    }

    @Test
    void searchRepositoriesShouldBuildQueryWithCreatedAfterOnly() {
        gitHubApiClient.searchRepositories(null, Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        for (String uri : allUris) {
            assertThat(uri).contains("created");
            assertThat(uri).doesNotContain("language");
        }
    }

    @Test
    void searchRepositoriesShouldBuildQueryWithBothFilters() {
        gitHubApiClient.searchRepositories("Python", Instant.parse("2024-06-01T00:00:00Z"), 2, 10);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        for (String uri : allUris) {
            assertThat(uri).contains("language:Python");
            assertThat(uri).contains("created");
        }
        String mainUri = allUris.get(2);
        assertThat(mainUri).contains("page=2");
        assertThat(mainUri).contains("per_page=10");
        assertThat(mainUri).contains("sort=stars");
        assertThat(mainUri).contains("order=desc");
    }

    @Test
    void searchRepositoriesShouldSetBearerToken() {
        gitHubApiClient.searchRepositories("Java", null, 1, 30);

        ArgumentCaptor<java.util.function.Consumer<HttpHeaders>> headerCaptor = ArgumentCaptor.forClass(java.util.function.Consumer.class);
        verify(headersSpec, times(3)).headers(headerCaptor.capture());
        HttpHeaders captured = new HttpHeaders();
        headerCaptor.getValue().accept(captured);
        assertThat(captured.get("Authorization")).containsExactly("Bearer test-token");
    }

    @Test
    void searchRepositoriesShouldReturnMultipleResults() {
        GitHubRepository repo1 = new GitHubRepository(1L, "repo-1", "owner/repo-1",
                "https://github.com/owner/repo-1", 500, 50, "Java",
                Instant.parse("2024-01-15T10:30:00Z"), Instant.parse("2024-06-01T14:20:00Z"));
        GitHubRepository repo2 = new GitHubRepository(2L, "repo-2", "owner/repo-2",
                "https://github.com/owner/repo-2", 300, 30, "Java",
                Instant.parse("2024-02-20T08:00:00Z"), Instant.parse("2024-07-15T12:00:00Z"));
        GitHubSearchResponse apiResponse = new GitHubSearchResponse(2, false, List.of(repo1, repo2));
        when(responseSpec.body(GitHubSearchResponse.class))
                .thenReturn(apiResponse)
                .thenReturn(apiResponse)
                .thenReturn(apiResponse);

        EnrichedSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).name()).isEqualTo("repo-1");
        assertThat(result.items().get(1).name()).isEqualTo("repo-2");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.maxStars()).isEqualTo(500);
        assertThat(result.maxForks()).isEqualTo(50);
    }

    @Test
    void searchRepositoriesShouldHandleBlankLanguage() {
        gitHubApiClient.searchRepositories("  ", Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        for (String uri : allUris) {
            assertThat(uri).doesNotContain("language");
        }
    }

    @Test
    void searchRepositoriesShouldDefaultToStarsQueryWhenNoFilters() {
        gitHubApiClient.searchRepositories(null, null, 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        for (String uri : allUris) {
            assertThat(uri).contains("q=stars:>0");
        }
    }

    @Test
    void searchRepositoriesShouldNotEncodeGreaterThanSign() {
        gitHubApiClient.searchRepositories("Java", Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        for (String uri : allUris) {
            assertThat(uri).contains("created:>2024-01-01");
            assertThat(uri).doesNotContain("%3E");
        }
    }

    @Test
    void searchRepositoriesShouldUseForksSortForMaxForks() {
        gitHubApiClient.searchRepositories("Java", null, 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        assertThat(allUris.get(0)).contains("sort=stars");
        assertThat(allUris.get(1)).contains("sort=forks");
        assertThat(allUris.get(2)).contains("sort=stars");
    }

    @Test
    void searchRepositoriesShouldUsePerPage1ForMaxQueries() {
        gitHubApiClient.searchRepositories("Java", null, 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec, times(3)).uri(uriCaptor.capture());
        List<String> allUris = uriCaptor.getAllValues();
        assertThat(allUris.get(0)).contains("per_page=1");
        assertThat(allUris.get(1)).contains("per_page=1");
        assertThat(allUris.get(2)).contains("per_page=30");
    }

    @Test
    void searchRepositoriesShouldReturnZeroMaxWhenMaxQueriesFail() {
        RestClientResponseException maxEx = mock(RestClientResponseException.class);
        when(maxEx.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(maxEx.getResponseBodyAsString()).thenReturn("{\"message\":\"error\"}");
        when(responseSpec.body(GitHubSearchResponse.class))
                .thenThrow(maxEx)
                .thenReturn(EMPTY_RESPONSE)
                .thenReturn(EMPTY_RESPONSE);

        EnrichedSearchResponse result = gitHubApiClient.searchRepositories("Java", null, 1, 30);

        assertThat(result.maxStars()).isEqualTo(0);
        assertThat(result.maxForks()).isEqualTo(0);
    }

    @Test
    void searchRepositoriesShouldThrowGitHubApiExceptionOn404() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(ex.getResponseBodyAsString()).thenReturn("{\"message\":\"Not Found\"}");
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API error")
                .hasMessageContaining("Not Found");
    }

    @Test
    void searchRepositoriesShouldThrowGitHubApiExceptionOn422() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        when(ex.getResponseBodyAsString()).thenReturn("{\"message\":\"Validation Failed\"}");
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API error")
                .hasMessageContaining("Validation Failed");
    }

    @Test
    void searchRepositoriesShouldThrowGitHubApiExceptionOn500() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(ex.getResponseBodyAsString()).thenReturn("{\"message\":\"Internal Server Error\"}");
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API error")
                .hasMessageContaining("Internal Server Error");
    }

    @Test
    void searchRepositoriesShouldThrowRateLimitExceptionOn403WithZeroRemaining() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);
        when(ex.getResponseBodyAsString()).thenReturn("{\"message\":\"API rate limit exceeded\"}");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Remaining", "0");
        headers.set("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(300).getEpochSecond()));
        when(ex.getResponseHeaders()).thenReturn(headers);
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubRateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded");
    }

    @Test
    void searchRepositoriesShouldThrowGitHubApiExceptionOn403WithoutRateLimitHeaders() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);
        when(ex.getResponseBodyAsString()).thenReturn("{\"message\":\"Forbidden\"}");
        HttpHeaders headers = new HttpHeaders();
        when(ex.getResponseHeaders()).thenReturn(headers);
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API error")
                .hasMessageContaining("Forbidden");
    }

    @Test
    void searchRepositoriesShouldHandleEmptyResponseBody() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(ex.getResponseBodyAsString()).thenReturn("");
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("Unknown GitHub API error");
    }

    @Test
    void searchRepositoriesShouldTruncateLongErrorMessage() {
        RestClientResponseException ex = mock(RestClientResponseException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(ex.getResponseBodyAsString()).thenReturn("A".repeat(300));
        when(responseSpec.body(GitHubSearchResponse.class)).thenThrow(ex);

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("Java", null, 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex2 -> {
                    String msg = ex2.getMessage();
                    assertThat(msg.length()).isLessThan(300);
                });
    }
}
