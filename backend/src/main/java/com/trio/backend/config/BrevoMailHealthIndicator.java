package com.trio.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("mailHealthIndicator")
@RequiredArgsConstructor
public class BrevoMailHealthIndicator implements HealthIndicator {

    private static final String BREVO_ACCOUNT_URL = "https://api.brevo.com/v3/account";

    private final MailProperties mailProperties;
    private final RestClient restClient;

    @Override
    public Health health() {
        if (mailProperties.getBrevoApiKey() == null || mailProperties.getBrevoApiKey().isBlank()) {
            return Health.down()
                    .withDetail("reason", "BREVO_API_KEY not configured; SMTP fallback in use, not monitored")
                    .build();
        }
        try {
            restClient.get()
                    .uri(BREVO_ACCOUNT_URL)
                    .header("api-key", mailProperties.getBrevoApiKey())
                    .retrieve()
                    .toBodilessEntity();
            return Health.up().build();
        } catch (Exception e) {
            log.warn("Brevo mail health check failed: {}", e.getMessage());
            return Health.down(e).build();
        }
    }
}