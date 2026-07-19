package com.reporanker.service;

import com.reporanker.dto.github.GitHubRepository;
import com.reporanker.dto.response.ScoredRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Computes popularity scores for GitHub repositories using a weighted formula:
 * stars (50%), forks (30%), and recency (20%) with exponential decay.
 * Scores are normalized via log-scaling and returned in descending order.
 */
@Service
public class ScoringService {

    private static final double WEIGHT_STARS = 0.50;
    private static final double WEIGHT_FORKS = 0.30;
    private static final double WEIGHT_RECENCY = 0.20;
    private static final double LAMBDA = 0.001;
    private static final double SCORE_SCALE = 100.0;

    public List<ScoredRepository> scoreAndRank(List<GitHubRepository> repositories) {
        if (repositories.isEmpty()) {
            return List.of();
        }

        int maxStars = repositories.stream().mapToInt(GitHubRepository::stars).max().orElse(1);
        int maxForks = repositories.stream().mapToInt(GitHubRepository::forks).max().orElse(1);
        Instant now = Instant.now();

        double logMaxStars = Math.log(1 + maxStars);
        double logMaxForks = Math.log(1 + maxForks);

        return repositories.stream()
                .map(repo -> {
                    double normalizedStars = logMaxStars > 0 ? Math.log(1 + repo.stars()) / logMaxStars : 0;
                    double normalizedForks = logMaxForks > 0 ? Math.log(1 + repo.forks()) / logMaxForks : 0;
                    long daysSinceUpdate = Duration.between(repo.updatedAt(), now).toDays();
                    double recencyScore = Math.exp(-LAMBDA * daysSinceUpdate);

                    double score = (WEIGHT_STARS * normalizedStars
                            + WEIGHT_FORKS * normalizedForks
                            + WEIGHT_RECENCY * recencyScore) * SCORE_SCALE;

                    return new ScoredRepository(
                            repo.id(),
                            repo.name(),
                            repo.fullName(),
                            repo.url(),
                            repo.stars(),
                            repo.forks(),
                            repo.language(),
                            repo.createdAt(),
                            repo.updatedAt(),
                            Math.round(score * 100.0) / 100.0
                    );
                })
                .sorted(Comparator.comparingDouble(ScoredRepository::score).reversed())
                .toList();
    }
}
