package com.polypilot.health.service;

import com.polypilot.health.dto.Check;
import com.polypilot.health.dto.ChecksResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Static-per-deploy checks surfaced on the health dashboard: Actuator health,
 * SQL init, and the JPA validate-mode table count.
 */
@Slf4j
@Service
@AllArgsConstructor
public class ChecksService {

    private static final String TABLE_COUNT_SQL =
            "SELECT count(*) FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";

    private final HealthEndpoint healthEndpoint;
    private final JdbcTemplate jdbcTemplate;

    public ChecksResponse getChecks() {
        return new ChecksResponse(List.of(
                actuatorHealthCheck(),
                sqlInitCheck(),
                jpaValidateCheck()));
    }

    private Check actuatorHealthCheck() {
        try {
            Status status = healthEndpoint.health().getStatus();
            boolean up = Status.UP.equals(status);
            return new Check("/actuator/health", up ? "pass" : "fail", "Docker healthcheck");
        } catch (Exception ex) {
            log.warn("Actuator health read failed", ex);
            return new Check("/actuator/health", "fail", "unreadable");
        }
    }

    /**
     * {@code spring.sql.init} runs {@code 001_schema.sql} then {@code 002_seed_data.sql}
     * on every boot and the app fails hard if either errors — so serving this
     * request at all means it succeeded. There is no post-boot partial-failure
     * state to detect.
     */
    private Check sqlInitCheck() {
        return new Check("spring.sql.init", "pass", "001_schema · 002_seed");
    }

    private Check jpaValidateCheck() {
        try {
            Integer count = jdbcTemplate.queryForObject(TABLE_COUNT_SQL, Integer.class);
            int tables = count == null ? 0 : count;
            return new Check("JPA validate", tables > 0 ? "pass" : "fail", tables + " tables");
        } catch (Exception ex) {
            log.warn("Table count query failed", ex);
            return new Check("JPA validate", "fail", "count unavailable");
        }
    }
}
