package com.polypilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Response body from ai-agent's {@code POST /ai/analyze}. */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class AnalyzeAccepted {
    String detail;

    @JsonProperty("market_id")
    String marketId;
}
