package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One sampled point on a market's probability / price history. Emitted
 * oldest-first by {@code GET /market/{marketId}/price-history}. Contract:
 * {@code contracts/market-price-history.md}.
 *
 * <p>{@code at} serializes as ISO-8601 UTC (same Jackson/JSR-310 path as
 * {@code MarketView.lastSyncedAt}); the {@code BigDecimal} fields serialize as
 * JSON numbers. {@code upPrice} / {@code downPrice} are probabilities in
 * {@code [0, 1]}, nullable (a gap the chart must break the line on).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPricePointView {

    private OffsetDateTime at;

    private BigDecimal upPrice;
    private BigDecimal downPrice;

    private BigDecimal volume24h;
    private BigDecimal liquidity;
}
