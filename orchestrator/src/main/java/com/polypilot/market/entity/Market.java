package com.polypilot.market.entity;

import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.enums.MarketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "markets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Market {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Column(name = "polymarket_condition_id", nullable = false, unique = true)
    private String polymarketConditionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MarketStatus status;

    @Column(name = "up_price", precision = 6, scale = 4)
    private BigDecimal upPrice;

    @Column(name = "down_price", precision = 6, scale = 4)
    private BigDecimal downPrice;

    @Column(name = "volume_24h", precision = 18, scale = 4)
    private BigDecimal volume24h;

    @Column(precision = 18, scale = 4)
    private BigDecimal liquidity;

    @Column(name = "resolution_date")
    private OffsetDateTime resolutionDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MarketOutcome outcome;

    /** Last time this row was reconciled against Polymarket by a sync job. */
    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}