package com.reporanker.dto.response;

/**
 * Response DTO for the home endpoint providing application metadata.
 *
 * @param name      the application name
 * @param version   the application version
 * @param buildTime the build timestamp
 */
public record HomeResponse(String name, String version, String buildTime) {
}
