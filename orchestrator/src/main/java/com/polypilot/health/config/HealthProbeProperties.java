package com.polypilot.health.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Timeouts for the {@code ServiceProbe} HTTP client that fans {@code GET /health}
 * out to the downstream Python services. Bound from the {@code health.probe.*}
 * block in {@code application.yml}; the inline defaults apply when the block is
 * absent so the probe is never left without a timeout.
 */
@ConfigurationProperties(prefix = "health.probe")
@Getter
@Setter
public class HealthProbeProperties {

    /** Max time to establish the TCP connection to a downstream {@code /health}. */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /** Max time to wait for the downstream {@code /health} response once connected. */
    private Duration readTimeout = Duration.ofSeconds(3);
}
