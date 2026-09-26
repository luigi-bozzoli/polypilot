package com.polypilot.strategy.dto.request;

import com.polypilot.enums.order.OrderType;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.strategy.StrategyNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body of {@code PUT /strategies/{id}} — a full replacement of the editable strategy fields,
 * same shape and Bean Validation constraints as {@link CreateStrategyRequest}. {@code id},
 * {@code userId}, {@code enabled}, {@code createdAt}, {@code updatedAt}, {@code deletedAt} are
 * deliberately absent — server-assigned/preserved, never client-controlled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStrategyRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotNull
    private MarketOutcome tokenSide;

    @NotNull
    private OrderType orderType;

    @NotNull
    private StrategyNode ruleTree;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal maxBetSize;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal maxDailyExposure;

    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal stopLossThreshold;

    @NotBlank
    private String cronExpression;

    @NotNull
    private UUID seriesId;

    @NotNull
    private Boolean dryRun;
}
