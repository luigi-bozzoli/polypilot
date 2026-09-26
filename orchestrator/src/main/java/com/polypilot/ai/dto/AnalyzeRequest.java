package com.polypilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Body for {@code POST /ai/analyze} on ai-agent. ai-agent has no DB of its own,
 * so the orchestrator passes the market context it already has. Field names
 * are snake_case to match ai-agent's pydantic {@code AnalyzeRequest} (no
 * alias generator configured there).
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class AnalyzeRequest {
    @JsonProperty("market_id")
    String marketId;

    @JsonProperty("asset_symbol")
    String assetSymbol;

    @JsonProperty("asset_display_name")
    String assetDisplayName;

    @JsonProperty("market_question")
    String marketQuestion;
}
