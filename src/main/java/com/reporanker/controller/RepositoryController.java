package com.reporanker.controller;

import com.reporanker.dto.response.PaginatedResponse;
import com.reporanker.dto.response.ScoredRepository;
import com.reporanker.service.RepositoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST controller for searching and ranking GitHub repositories.
 * Supports filtering by language and creation date, with paginated results.
 */
@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    /**
     * Constructs a RepositoryController with the given service.
     *
     * @param repositoryService the service for searching and ranking repositories
     */
    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Searches and ranks GitHub repositories with optional filters and pagination.
     *
     * @param language    filter by programming language (optional)
     * @param createdAfter filter by creation date in ISO-8601 format (optional)
     * @param page        the page number (default: 1)
     * @param perPage     the number of results per page (default: 30, max: 100)
     * @return paginated response containing scored repositories
     */
    @GetMapping
    public PaginatedResponse<ScoredRepository> search(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage) {
        return repositoryService.searchAndRank(language, createdAfter, page, perPage);
    }
}
