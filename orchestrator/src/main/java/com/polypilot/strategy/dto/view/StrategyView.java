package com.polypilot.strategy.dto.view;

import com.polypilot.enums.order.OrderType;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.strategy.StrategyNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound view of one {@link com.polypilot.entity.Strategy}. {@code ruleTree} is the parsed
 * {@link StrategyNode} tree, not the raw {@code rule_tree} JSONB string — it (de)serializes
 * through {@link com.polypilot.strategy.json.StrategyNodeJacksonModule}'s registered
 * serializer, matching the discriminated-union shape
 * {@code dashboard/src/features/strategies/types.ts}'s {@code RuleNode} expects. Body of
 * {@code GET /api/strategies} (list) and {@code GET /api/strategies/{id}} (detail).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyView {

    private UUID id;
    private String name;
    private String description;
    private Boolean enabled;
    private Boolean dryRun;
    private String cronExpression;
    private UUID seriesId;
    private String seriesTitle;
    private MarketOutcome tokenSide;
    private OrderType orderType;
    private BigDecimal maxBetSize;
    private BigDecimal maxDailyExposure;
    private BigDecimal stopLossThreshold;
    private StrategyNode ruleTree;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Total orders placed by this strategy — set by {@code StrategyService.listStrategies} via a
     * batched count query, not part of the entity mapping (MapStruct leaves it at its default 0
     * on {@code getStrategy}/{@code create}/{@code update}, which don't populate it).
     */
    private int tradeCount;
}
