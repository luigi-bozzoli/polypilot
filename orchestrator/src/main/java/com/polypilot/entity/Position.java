package com.polypilot.entity;

import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.enums.order.PositionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "positions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_position_key",
                        columnNames = {
                                "user_id",
                                "market_id",
                                "token_side",
                                "is_dry_run",
                                "status"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_side", nullable = false, length = 3)
    private MarketOutcome tokenSide;

    @Column(name = "size", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal size = BigDecimal.ZERO;

    @Column(name = "avg_entry_price", nullable = false, precision = 6, scale = 4)
    private BigDecimal avgEntryPrice;

    @Column(name = "current_price", precision = 6, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "unrealized_pnl", precision = 18, scale = 4)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PositionStatus status;

    @Column(name = "is_dry_run", nullable = false)
    @Builder.Default
    private Boolean isDryRun = true;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}