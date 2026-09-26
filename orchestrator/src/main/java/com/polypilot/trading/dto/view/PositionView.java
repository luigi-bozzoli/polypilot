package com.polypilot.trading.dto.view;

import com.polypilot.market.enums.MarketOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row of the caller's open positions, body of {@code GET /positions}. Backs the Overview
 * "Open positions" widget. {@code marketQuestion} is denormalized via batch lookup (same pattern
 * as {@code MarketOrderView}/{@code StrategyOrderView}) so the card doesn't need a second fetch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionView {

    private UUID id;
    private String marketId;
    private String marketQuestion;
    private MarketOutcome tokenSide;
    private BigDecimal size;
    private BigDecimal avgEntryPrice;
    private BigDecimal unrealizedPnl;
    private OffsetDateTime openedAt;
}
