package com.polypilot.health.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.Map;

/**
 * Body of the aggregate {@code GET /health} (see {@code contracts/health-service-metrics.md}):
 * the orchestrator's own status and metrics, plus a fanned-out probe of each
 * downstream Python service.
 *
 * <ul>
 *   <li>{@code service} — always {@code "orchestrator"}</li>
 *   <li>{@code status} — {@code "ok"}</li>
 *   <li>{@code metrics} — orchestrator uptime / heap; {@code latencyMs} is null (it
 *       does not probe itself over HTTP)</li>
 *   <li>{@code db} — {@code "ok"} or {@code "down"}</li>
 *   <li>{@code scheduler} — {@code "running"} or {@code "stopped"}</li>
 *   <li>{@code auth-service} / {@code ai-agent} — each downstream's own {@code /health}
 *       body, passed through verbatim (an open {@code {status, metrics?, ...}} shape),
 *       or the {@code {status: "unreachable", error: ...}} stub when the probe fails</li>
 * </ul>
 */
@Value
public class HealthResponse {
    String service;
    String status;
    ServiceMetrics metrics;
    String db;
    String scheduler;

    @JsonProperty("auth-service")
    Map<String, Object> authService;

    @JsonProperty("ai-agent")
    Map<String, Object> aiAgent;
}
