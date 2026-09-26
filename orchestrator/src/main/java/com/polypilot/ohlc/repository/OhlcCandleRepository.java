package com.polypilot.ohlc.repository;

import com.polypilot.ohlc.entity.OhlcCandle;
import com.polypilot.ohlc.entity.OhlcCandleId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OhlcCandleRepository extends JpaRepository<OhlcCandle, OhlcCandleId> {

    /**
     * The most recent closed candles for a stream, newest-first, capped by
     * {@code pageable}'s page size — the read path for indicator calculation.
     * Forming candles are excluded so a value never shifts retroactively mid-bar.
     * Callers building a chronological {@link org.ta4j.core.BarSeries} must
     * reverse this list to ascending order first.
     */
    @Query("""
            select c from OhlcCandle c
            where c.id.symbol = :symbol and c.id.timeframe = :timeframe and c.closed = true
            order by c.id.openTime desc
            """)
    List<OhlcCandle> findRecentClosedCandles(@Param("symbol") String symbol,
                                             @Param("timeframe") String timeframe,
                                             Pageable pageable);

    /** Latest stored {@code open_time} for a stream, or empty on cold start. */
    @Query("""
            select max(c.id.openTime) from OhlcCandle c
            where c.id.symbol = :symbol and c.id.timeframe = :timeframe
            """)
    Optional<OffsetDateTime> findMaxOpenTime(@Param("symbol") String symbol,
                                             @Param("timeframe") String timeframe);

    /**
     * Oldest stored candle that is not yet closed — the anchor for the forward
     * fetch, so every still-forming row is re-pulled until it is finalised.
     */
    @Query("""
            select min(c.id.openTime) from OhlcCandle c
            where c.id.symbol = :symbol and c.id.timeframe = :timeframe and c.closed = false
            """)
    Optional<OffsetDateTime> findEarliestFormingOpenTime(@Param("symbol") String symbol,
                                                         @Param("timeframe") String timeframe);

    /**
     * The most recent candles for a stream, newest-first, capped by {@code pageable}'s page
     * size — the read path for the OHLC chart. Unlike {@link #findRecentClosedCandles}, this
     * includes the still-forming candle (if any), since the chart is expected to show a live
     * rightmost bar. Callers must reverse this list to ascending order before charting, same
     * convention as {@link #findRecentClosedCandles}.
     */
    @Query("""
            select c from OhlcCandle c
            where c.id.symbol = :symbol and c.id.timeframe = :timeframe
            order by c.id.openTime desc
            """)
    List<OhlcCandle> findRecentCandles(@Param("symbol") String symbol,
                                       @Param("timeframe") String timeframe,
                                       Pageable pageable);

    /** Every stored {@code open_time} for a stream, ascending — for gap detection. */
    @Query("""
            select c.id.openTime from OhlcCandle c
            where c.id.symbol = :symbol and c.id.timeframe = :timeframe
            order by c.id.openTime asc
            """)
    List<OffsetDateTime> findOpenTimesAsc(@Param("symbol") String symbol,
                                          @Param("timeframe") String timeframe);

    /**
     * Insert a candle, or overwrite it only while it is still forming. The
     * {@code WHERE ohlc_candles.is_closed = false} guard on the {@code DO UPDATE}
     * makes a stored closed candle immutable: re-fetching it is a no-op.
     */
    @Modifying
    @Query(value = """
            INSERT INTO ohlc_candles
              (symbol, timeframe, open_time, open, high, low, close, volume, close_time, is_closed, updated_at)
            VALUES
              (:symbol, :timeframe, :openTime, :open, :high, :low, :close, :volume, :closeTime, :closed, now())
            ON CONFLICT (symbol, timeframe, open_time) DO UPDATE SET
              open       = EXCLUDED.open,
              high       = EXCLUDED.high,
              low        = EXCLUDED.low,
              close      = EXCLUDED.close,
              volume     = EXCLUDED.volume,
              close_time = EXCLUDED.close_time,
              is_closed  = EXCLUDED.is_closed,
              updated_at = now()
            WHERE ohlc_candles.is_closed = false
            """, nativeQuery = true)
    void upsert(@Param("symbol") String symbol,
                @Param("timeframe") String timeframe,
                @Param("openTime") OffsetDateTime openTime,
                @Param("open") BigDecimal open,
                @Param("high") BigDecimal high,
                @Param("low") BigDecimal low,
                @Param("close") BigDecimal close,
                @Param("volume") BigDecimal volume,
                @Param("closeTime") OffsetDateTime closeTime,
                @Param("closed") boolean closed);
}
