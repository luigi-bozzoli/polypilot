package com.polypilot.health.dto;

import lombok.Value;

import java.util.List;

/** Body of {@code GET /health/infrastructure}. */
@Value
public class InfrastructureResponse {
    List<InfraComponent> components;
}
