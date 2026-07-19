package com.reporanker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration enabling Spring's annotation-driven caching support
 * backed by Caffeine with a configurable time-to-live.
 */
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "app.cache")
public class CacheConfig {

    private int ttlMinutes = 5;

    /**
     * Returns the cache TTL in minutes.
     *
     * @return the TTL in minutes
     */
    public int getTtlMinutes() {
        return ttlMinutes;
    }

    /**
     * Sets the cache TTL in minutes.
     *
     * @param ttlMinutes the TTL in minutes
     */
    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * Creates a Caffeine-backed cache manager with the configured TTL.
     *
     * @return the cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("repositories");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES));
        return manager;
    }
}
