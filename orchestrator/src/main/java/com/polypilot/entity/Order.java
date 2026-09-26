package com.polypilot.entity;

import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.enums.order.OrderStatus;
import com.polypilot.enums.order.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "strategy_id", nullable = false)
    private UUID strategyId;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "external_order_id", unique = true, length = 255)
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_side", nullable = false, length = 3)
    private MarketOutcome tokenSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Column(name = "size_requested", nullable = false, precision = 18, scale = 4)
    private BigDecimal sizeRequested;

    @Column(name = "size_filled", nullable = false, precision = 18, scale = 4)
    private BigDecimal sizeFilled;

    @Column(name = "price", nullable = false, precision = 6, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "is_dry_run", nullable = false)
    private Boolean isDryRun;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "placed_at", nullable = false)
    private OffsetDateTime placedAt;

    @Column(name = "filled_at")
    private OffsetDateTime filledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
