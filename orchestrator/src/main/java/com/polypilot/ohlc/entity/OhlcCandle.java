package com.polypilot.ohlc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A Binance OHLCV candle. One row per {@code (symbol, timeframe, open_time)}.
 *
 * <p>Written only by the {@code ohlc-sync} job, and only through the native
 * upsert on {@link com.polypilot.ohlc.repository.OhlcCandleRepository} — closed
 * candles are immutable, the forming candle is overwritten each sync. This
 * mapping exists for {@code ddl-auto=validate} and for future read paths
 * (indicator calculation).
 */
@Entity
@Table(name = "ohlc_candles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OhlcCandle {

    @EmbeddedId
    private OhlcCandleId id;

    @Column(name = "open", nullable = false, precision = 20, scale = 8)
    private BigDecimal open;

    @Column(name = "high", nullable = false, precision = 20, scale = 8)
    private BigDecimal high;

    @Column(name = "low", nullable = false, precision = 20, scale = 8)
    private BigDecimal low;

    @Column(name = "close", nullable = false, precision = 20, scale = 8)
    private BigDecimal close;

    @Column(name = "volume", nullable = false, precision = 30, scale = 8)
    private BigDecimal volume;

    @Column(name = "close_time", nullable = false)
    private OffsetDateTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
