package com.polypilot.market.entity;

import com.polypilot.reference.entity.Ticker;
import com.polypilot.series.enums.SeriesType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Series {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String polymarketId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ticker;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recurrence;

    @Enumerated(EnumType.STRING)
    @Column(name = "series_type", nullable = false, length = 50)
    private SeriesType seriesType;

    /**
     * Whether the sync jobs pull markets for this series and the strategy engine
     * evaluates them. Set by the user (no API yet); the five seeded series are
     * tracked out of the box.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean tracked = false;

    /**
     * The asset this series is about (BTC/ETH). Nullable — existing rows are not
     * backfilled; populated going forward. Named {@code asset} because the
     * legacy {@link #ticker} field above is actually a slug, not a symbol.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker asset;
}