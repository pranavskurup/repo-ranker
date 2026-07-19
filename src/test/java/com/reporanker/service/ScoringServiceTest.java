package com.reporanker.service;

import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.response.ScoredRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    private GitHubRepository createRepo(int stars, int forks, int daysAgo) {
        Instant now = Instant.now();
        return new GitHubRepository(
                1L, "test-repo", "owner/test-repo",
                "https://github.com/owner/test-repo",
                stars, forks, "Java",
                now.minus(365, ChronoUnit.DAYS),
                now.minus(daysAgo, ChronoUnit.DAYS)
        );
    }

    @Test
    void scoreAndRankShouldReturnEmptyListForEmptyInput() {
        List<ScoredRepository> result = scoringService.scoreAndRank(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void scoreAndRankShouldReturnSingleRepo() {
        GitHubRepository repo = createRepo(100, 20, 5);
        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(repo));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().score()).isGreaterThan(0);
    }

    @Test
    void scoreAndRankShouldRankHigherStarsHigher() {
        GitHubRepository lowStars = createRepo(10, 10, 1);
        GitHubRepository highStars = createRepo(1000, 10, 1);

        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(lowStars, highStars));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().stars()).isEqualTo(1000);
        assertThat(result.getLast().stars()).isEqualTo(10);
    }

    @Test
    void scoreAndRankShouldRankHigherForksHigher() {
        GitHubRepository lowForks = createRepo(100, 5, 1);
        GitHubRepository highForks = createRepo(100, 500, 1);

        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(lowForks, highForks));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().forks()).isEqualTo(500);
    }

    @Test
    void scoreAndRankShouldRankRecentlyUpdatedHigher() {
        GitHubRepository oldUpdate = createRepo(100, 50, 365);
        GitHubRepository recentUpdate = createRepo(100, 50, 1);

        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(oldUpdate, recentUpdate));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().updatedAt()).isAfter(result.getLast().updatedAt());
    }

    @Test
    void scoreAndRankShouldUseLogNormalization() {
        GitHubRepository repo1 = createRepo(10, 5, 1);
        GitHubRepository repo2 = createRepo(10000, 5, 1);

        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(repo1, repo2));

        assertThat(result).hasSize(2);
        // repo2 has 1000x more stars but score should not be 1000x higher due to log normalization
        double ratio = result.getFirst().score() / result.getLast().score();
        assertThat(ratio).isLessThan(10);
    }

    @Test
    void scoreShouldBeBetween0And100() {
        GitHubRepository repo = createRepo(500, 100, 30);
        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(repo));

        assertThat(result.getFirst().score()).isBetween(0.0, 100.0);
    }

    @Test
    void scoreAndRankShouldSortDescendingByScore() {
        GitHubRepository repo1 = createRepo(10, 5, 365);
        GitHubRepository repo2 = createRepo(500, 100, 10);
        GitHubRepository repo3 = createRepo(100, 50, 30);

        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(repo1, repo2, repo3));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).score()).isGreaterThanOrEqualTo(result.get(1).score());
        assertThat(result.get(1).score()).isGreaterThanOrEqualTo(result.get(2).score());
    }

    @Test
    void scoreShouldUseCorrectWeights() {
        // Repo with only stars (high stars, low forks, old update)
        GitHubRepository starsOnly = createRepo(10000, 0, 365);
        // Repo with only forks (low stars, high forks, old update)
        GitHubRepository forksOnly = createRepo(0, 10000, 365);

        List<ScoredRepository> result = scoringService.scoreAndRank(List.of(starsOnly, forksOnly));

        assertThat(result).hasSize(2);
        // Stars weighted 0.50 vs forks 0.30, so stars repo should score higher
        assertThat(result.getFirst().stars()).isEqualTo(10000);
    }
}
