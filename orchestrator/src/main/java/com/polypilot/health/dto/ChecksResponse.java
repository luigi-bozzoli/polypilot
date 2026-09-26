package com.polypilot.health.dto;

import lombok.Value;

import java.util.List;

/** Body of {@code GET /health/checks}. */
@Value
public class ChecksResponse {
    List<Check> checks;
}
