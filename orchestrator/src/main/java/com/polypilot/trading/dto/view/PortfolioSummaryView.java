package com.polypilot.trading.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Body of {@code GET /portfolio/summary} — backs the Overview stat row. Deliberately has no
 * "equity" figure: nothing in the schema stores a starting-capital baseline, so only the
 * aggregates {@code positions}/{@code orders} can actually support are exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryView {

    /** Sum of {@code unrealized_pnl} across the caller's open positions. */
    private BigDecimal unrealizedPnl;

    /** Sum of {@code realized_pnl} across positions the caller closed in the last 7 days. */
    private BigDecimal realizedPnl7d;

    /** Count of the caller's orders placed today (UTC). */
    private long dryOrdersToday;

    private int openPositionCount;
}
