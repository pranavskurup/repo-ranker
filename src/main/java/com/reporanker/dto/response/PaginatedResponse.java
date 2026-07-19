package com.reporanker.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        @JsonProperty("total_count") int totalCount,
        @JsonProperty("current_page") int currentPage,
        @JsonProperty("per_page") int perPage,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("has_next") boolean hasNext,
        @JsonProperty("has_previous") boolean hasPrevious
) {
    public static <T> PaginatedResponse<T> of(List<T> items, int totalCount, int currentPage, int perPage) {
        int totalPages = perPage > 0 ? (int) Math.ceil((double) totalCount / perPage) : 0;
        return new PaginatedResponse<>(items, totalCount, currentPage, perPage, totalPages, currentPage < totalPages, currentPage > 1);
    }
}
