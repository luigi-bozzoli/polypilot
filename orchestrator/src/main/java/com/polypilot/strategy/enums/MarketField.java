package com.polypilot.strategy.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A static field on {@code markets}/{@code sentiment_scores} that a strategy's
 * {@link com.polypilot.strategy.MarketFieldCompareNode} can compare against —
 * as opposed to a computed technical indicator. Wire values (the {@code @JsonProperty}
 * on each constant) match the {@code field} values in
 * {@code create-strategy-api-contracts.md}'s rule-tree contract exactly.
 */
public enum MarketField {
    @JsonProperty("up_price") UP_PRICE,
    @JsonProperty("down_price") DOWN_PRICE,
    @JsonProperty("volume_24h") VOLUME_24H,
    @JsonProperty("liquidity") LIQUIDITY,
    @JsonProperty("sentiment") SENTIMENT,
    @JsonProperty("sentiment.confidence") SENTIMENT_CONFIDENCE
}
