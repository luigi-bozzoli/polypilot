package com.polypilot.reference.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A tradeable / trackable asset. DB-backed replacement for the old
 * {@code binance.symbols} yaml list — the {@code ohlc-sync} job syncs one candle
 * stream per enabled ticker × enabled {@link Timeframe}. Managed directly in the
 * DB; there is no CRUD API yet.
 */
@Entity
@Table(name = "ticker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticker {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    /** Canonical short symbol, e.g. {@code BTC}. */
    @Column(nullable = false, length = 20, unique = true)
    private String symbol;

    /** Binance Spot pair used for kline requests, e.g. {@code BTCUSDT}. */
    @Column(name = "binance_symbol", nullable = false, length = 20)
    private String binanceSymbol;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "base_asset", nullable = false, length = 20)
    private String baseAsset;

    @Column(name = "quote_asset", nullable = false, length = 20)
    private String quoteAsset;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
