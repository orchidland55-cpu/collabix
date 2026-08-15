package com.trio.backend.integration.projects;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Replaces production rate limiters with high-capacity buckets for integration tests.
 */
@Configuration
public class IntegrationTestRateLimitConfig {

    private static Bucket highCapacityBucket() {
        Bandwidth limit = Bandwidth.classic(1_000_000, Refill.greedy(1_000_000, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Bean
    @Primary
    Bucket globalRateLimiter() {
        return highCapacityBucket();
    }

    @Bean
    Bucket aiRateLimiter() {
        return highCapacityBucket();
    }

    @Bean
    Bucket authRateLimiter() {
        return highCapacityBucket();
    }
}
