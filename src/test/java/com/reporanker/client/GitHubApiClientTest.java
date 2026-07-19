package com.reporanker.client;

import com.reporanker.config.GitHubApiProperties;
import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.github.GitHubSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GitHubApiClientTest {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.RequestHeadersSpec headersSpec;
    private RestClient.ResponseSpec responseSpec;
    private GitHubApiClient gitHubApiClient;

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
    }

    @Test
    void searchRepositoriesShouldReturnResults() {
        GitHubRepository repo = new GitHubRepository(1L, "test-repo", "owner/test-repo",
                "https://github.com/owner/test-repo", 100, 20, "Java",
                Instant.parse("2024-01-15T10:30:00Z"), Instant.parse("2024-06-01T14:20:00Z"));
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(1, false, List.of(repo));
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(searchResponse);

        GitHubSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().name()).isEqualTo("test-repo");
        assertThat(result.items().getFirst().stars()).isEqualTo(100);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void searchRepositoriesShouldReturnEmptyListWhenResponseIsNull() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(null);

        GitHubSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    @Test
    void searchRepositoriesShouldReturnEmptyListWhenItemsIsNull() {
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(0, false, null);
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(searchResponse);

        GitHubSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void searchRepositoriesShouldBuildQueryWithLanguageOnly() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories("Java", null, 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue()).contains("q=language:Java");
        assertThat(uriCaptor.getValue()).doesNotContain("created");
    }

    @Test
    void searchRepositoriesShouldBuildQueryWithCreatedAfterOnly() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories(null, Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue()).contains("created");
        assertThat(uriCaptor.getValue()).doesNotContain("language");
    }

    @Test
    void searchRepositoriesShouldBuildQueryWithBothFilters() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories("Python", Instant.parse("2024-06-01T00:00:00Z"), 2, 10);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();
        assertThat(uri).contains("language:Python");
        assertThat(uri).contains("created");
        assertThat(uri).contains("page=2");
        assertThat(uri).contains("per_page=10");
        assertThat(uri).contains("sort=stars");
        assertThat(uri).contains("order=desc");
    }

    @Test
    void searchRepositoriesShouldSetBearerToken() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories("Java", null, 1, 30);

        ArgumentCaptor<java.util.function.Consumer<HttpHeaders>> headerCaptor = ArgumentCaptor.forClass(java.util.function.Consumer.class);
        verify(headersSpec).headers(headerCaptor.capture());
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
        GitHubSearchResponse searchResponse = new GitHubSearchResponse(2, false, List.of(repo1, repo2));
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(searchResponse);

        GitHubSearchResponse result = gitHubApiClient.searchRepositories("Java",
                Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).name()).isEqualTo("repo-1");
        assertThat(result.items().get(1).name()).isEqualTo("repo-2");
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void searchRepositoriesShouldHandleBlankLanguage() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories("  ", Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue()).doesNotContain("language");
    }

    @Test
    void searchRepositoriesShouldDefaultToStarsQueryWhenNoFilters() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories(null, null, 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue()).contains("q=stars:>0");
    }

    @Test
    void searchRepositoriesShouldNotEncodeGreaterThanSign() {
        when(responseSpec.body(GitHubSearchResponse.class)).thenReturn(new GitHubSearchResponse(0, false, List.of()));

        gitHubApiClient.searchRepositories("Java", Instant.parse("2024-01-01T00:00:00Z"), 1, 30);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue()).contains("created:>2024-01-01");
        assertThat(uriCaptor.getValue()).doesNotContain("%3E");
    }
}
