package com.reporanker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for Spring's {@link RestClient} used by external API clients.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a {@link RestClient.Builder} bean for external HTTP calls.
     *
     * @return a new RestClient builder instance
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
