package com.polypilot.reference.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * A supported candle interval. DB-backed replacement for the old
 * {@code binance.timeframes} yaml list. {@link #code} is the Binance interval
 * code verbatim ({@code 1m}, {@code 1h}, {@code 1M}, …) and is used as the
 * natural primary key. Managed directly in the DB; there is no CRUD API yet.
 */
@Entity
@Table(name = "timeframe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timeframe {

    @Id
    @Column(length = 8)
    private String code;

    @Column(nullable = false, length = 40)
    private String label;

    /** Nominal candle length in ms; {@code null} for calendar-based intervals ({@code 1M}). */
    @Column(name = "milliseconds")
    private Long milliseconds;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
