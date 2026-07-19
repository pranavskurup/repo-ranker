package com.reporanker.controller;

import com.reporanker.dto.response.HomeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing application health and version information.
 */
@RestController
public class HomeController {

    @Value("${app.name}")
    private String name;

    @Value("${app.version}")
    private String version;

    @Value("${app.build-time}")
    private String buildTime;

    /**
     * Returns application name, version, and build time.
     *
     * @return the home response with application metadata
     */
    @GetMapping("/")
    public HomeResponse home() {
        return new HomeResponse(name, version, buildTime);
    }
}
