package com.polypilot.health.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

/**
 * One infrastructure component's status.
 *
 * <ul>
 *   <li>{@code name} — display name, e.g. {@code "Postgres 16"}</li>
 *   <li>{@code status} — {@code "ready"}, {@code "degraded"} or {@code "unreachable"};
 *       drives the status-dot colour</li>
 *   <li>{@code label} — optional display word when it needs to differ from
 *       {@code status} (e.g. {@code "connected"} for RabbitMQ); omitted when unset</li>
 *   <li>{@code port} — port the service is reachable on inside {@code polypilot-net}</li>
 *   <li>{@code detail} — single display-ready line of service-specific detail</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Value
public class InfraComponent {
    String name;
    String status;
    String label;
    int port;
    String detail;
}
