package com.polypilot.market.dto.view;

import com.polypilot.enums.order.OrderStatus;
import com.polypilot.market.enums.MarketOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row of the market-scoped recent-orders feed, body of {@code GET /market/{marketId}/orders}.
 * Mirrors {@code StrategyOrderView} field-for-field, but denormalizes the owning strategy's
 * {@code strategyName} instead of the market question (the market is already known from the
 * request). Contract: {@code contracts/market-recent-orders.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrderView {

    private UUID id;
    private OffsetDateTime placedAt;
    private UUID strategyId;
    private String strategyName;
    private MarketOutcome tokenSide;
    private BigDecimal sizeRequested;
    private BigDecimal sizeFilled;
    private BigDecimal price;
    private OrderStatus status;
    private Boolean isDryRun;
}
