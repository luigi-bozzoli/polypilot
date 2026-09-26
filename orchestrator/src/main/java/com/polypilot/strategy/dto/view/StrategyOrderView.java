package com.polypilot.strategy.dto.view;

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
 * One row of the strategy-scoped recent-orders feed, body of
 * {@code GET /strategies/{id}/orders}. Mirrors the {@code Order} entity field-for-field
 * (real {@code OrderStatus}, {@code sizeRequested}/{@code sizeFilled} kept separate) plus
 * the denormalized {@code marketQuestion} so {@code StrategyRecentOrdersCard} doesn't need
 * a second fetch. Contract: {@code contracts/strategy-recent-orders.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyOrderView {

    private UUID id;
    private OffsetDateTime placedAt;
    private String marketId;
    private String marketQuestion;
    private MarketOutcome tokenSide;
    private BigDecimal sizeRequested;
    private BigDecimal sizeFilled;
    private BigDecimal price;
    private OrderStatus status;
    private Boolean isDryRun;
}
