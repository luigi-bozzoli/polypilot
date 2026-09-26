package com.polypilot.market.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceSnapshot {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Column(name = "up_price", precision = 6, scale = 4)
    private BigDecimal yesPrice;

    @Column(name = "down_price", precision = 6, scale = 4)
    private BigDecimal noPrice;

    @Column(name = "volume_24h", precision = 18, scale = 4)
    private BigDecimal volume24h;

    @Column(name = "liquidity", precision = 18, scale = 4)
    private BigDecimal liquidity;

    @Column(name = "snapshot_at", nullable = false)
    private OffsetDateTime snapshotAt;
}
