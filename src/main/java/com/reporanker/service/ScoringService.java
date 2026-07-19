package com.reporanker.service;

import com.reporanker.config.ScoringProperties;
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
 * Scores are normalized via log-scaling against configurable max values
 * and returned in descending order.
 */
@Service
public class ScoringService {

    private static final double WEIGHT_STARS = 0.50;
    private static final double WEIGHT_FORKS = 0.30;
    private static final double WEIGHT_RECENCY = 0.20;
    private static final double LAMBDA = 0.001;
    private static final double SCORE_SCALE = 100.0;

    private final double logMaxStars;
    private final double logMaxForks;

    /**
     * Constructs a ScoringService with the given normalization bounds.
     *
     * @param properties the scoring configuration properties
     */
    public ScoringService(ScoringProperties properties) {
        this.logMaxStars = Math.log(1 + properties.maxStars());
        this.logMaxForks = Math.log(1 + properties.maxForks());
    }

    /**
     * Scores and ranks the given repositories by popularity.
     * Uses log-normalized stars (50%), forks (30%), and exponential decay recency (20%).
     *
     * @param repositories the list of GitHub repositories to score
     * @return sorted list of scored repositories in descending score order
     */
    public List<ScoredRepository> scoreAndRank(List<GitHubRepository> repositories) {
        if (repositories.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();

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
