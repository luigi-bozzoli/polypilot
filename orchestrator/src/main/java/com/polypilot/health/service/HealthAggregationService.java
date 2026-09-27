package com.polypilot.health.service;

import com.polypilot.health.dto.HealthResponse;
import com.polypilot.health.dto.ServiceMetrics;
import com.polypilot.health.support.JvmMetrics;
import com.polypilot.health.support.UptimeFormatter;
import com.polypilot.market.service.JobScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Builds the aggregate {@code GET /health} body: the orchestrator's own status
 * and metrics, plus a fanned-out probe of each downstream Python service.
 */
@Slf4j
@Service
public class HealthAggregationService {

    private final ServiceProbe serviceProbe;
    private final JdbcTemplate jdbcTemplate;
    private final JobScheduler jobScheduler;
    private final String authHealthUrl;
    private final String aiHealthUrl;

    public HealthAggregationService(ServiceProbe serviceProbe,
                                    JdbcTemplate jdbcTemplate,
                                    JobScheduler jobScheduler,
                                    @Value("${services.auth-url}") String authUrl,
                                    @Value("${services.ai-url}") String aiUrl) {
        this.serviceProbe = serviceProbe;
        this.jdbcTemplate = jdbcTemplate;
        this.jobScheduler = jobScheduler;
        this.authHealthUrl = authUrl + "/health";
        this.aiHealthUrl = aiUrl + "/health";
    }

    public HealthResponse health() {
        return new HealthResponse(
                "orchestrator",
                "ok",
                selfMetrics(),
                dbStatus(),
                jobScheduler.isRunning() ? "running" : "stopped",
                serviceProbe.probe(authHealthUrl),
                serviceProbe.probe(aiHealthUrl));
    }

    /** {@code latencyMs} is null for the orchestrator — it does not probe itself over HTTP. */
    private ServiceMetrics selfMetrics() {
        return new ServiceMetrics(
                UptimeFormatter.format(JvmMetrics.uptime()),
                null,
                JvmMetrics.heapDetail());
    }

    private String dbStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "ok";
        } catch (Exception ex) {
            log.warn("DB health check failed", ex);
            return "down";
        }
    }
}
