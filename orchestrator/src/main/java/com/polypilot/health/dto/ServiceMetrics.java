package com.polypilot.health.dto;

import lombok.Value;

/**
 * Per-service health metrics row (see {@code contracts/health-service-metrics.md}).
 *
 * <ul>
 *   <li>{@code uptime} — pre-formatted wall-clock uptime, e.g. {@code "3d 04h"}</li>
 *   <li>{@code latencyMs} — health-probe round trip in ms; {@code null} when not
 *       measured (the orchestrator does not probe itself)</li>
 *   <li>{@code detail} — one short, already-formatted service-specific fragment</li>
 * </ul>
 */
@Value
public class ServiceMetrics {
    String uptime;
    Long latencyMs;
    String detail;
}
