package com.polypilot.health;

import com.polypilot.health.dto.ChecksResponse;
import com.polypilot.health.dto.HealthResponse;
import com.polypilot.health.dto.InfrastructureResponse;
import com.polypilot.health.service.ChecksService;
import com.polypilot.health.service.HealthAggregationService;
import com.polypilot.health.service.InfrastructureService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health surface for the dashboard's {@code /health} page.
 *
 * <ul>
 *   <li>{@code GET /health} — aggregate: orchestrator status + metrics, fanned
 *       out to both Python services. Polled every 5s by the dashboard.</li>
 *   <li>{@code GET /health/checks} — static-per-deploy schema / boot checks.</li>
 *   <li>{@code GET /health/infrastructure} — Postgres / Redis / RabbitMQ status.</li>
 * </ul>
 *
 * <p>All three require a valid session (see {@code SecurityConfig}); the
 * Docker healthcheck uses {@code /actuator/health}, which stays public.
 */
@RestController
@RequestMapping("/health")
@AllArgsConstructor
public class HealthController {

    private final HealthAggregationService healthAggregationService;
    private final ChecksService checksService;
    private final InfrastructureService infrastructureService;

    @GetMapping
    public HealthResponse health() {
        return healthAggregationService.health();
    }

    @GetMapping("/checks")
    public ChecksResponse checks() {
        return checksService.getChecks();
    }

    @GetMapping("/infrastructure")
    public InfrastructureResponse infrastructure() {
        return infrastructureService.getInfrastructure();
    }
}
