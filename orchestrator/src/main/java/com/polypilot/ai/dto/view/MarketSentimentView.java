package com.polypilot.ai.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response of {@code GET /market/{marketId}/sentiment/latest}. 
 *
 * <p>{@code score} (signed -1..1) always serializes as {@code null}:
 * {@code sentiment_scores} only stores the BULLISH/BEARISH/NEUTRAL label plus
 * a 0..1 confidence magnitude, no separate signed scalar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSentimentView {
    private UUID id;
    private String label;
    private BigDecimal confidence;
    private BigDecimal score;
    private Integer articleCount;
    private String reasoning;
    private String modelUsed;
    private OffsetDateTime scoredAt;
}
