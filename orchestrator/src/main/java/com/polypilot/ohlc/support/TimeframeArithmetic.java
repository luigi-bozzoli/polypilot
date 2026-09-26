package com.polypilot.ohlc.support;

import com.polypilot.reference.entity.Timeframe;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Timeframe arithmetic for the OHLC sync, driven by the DB-backed
 * {@link Timeframe} row rather than a hardcoded code table.
 *
 * <p>{@link Timeframe#getMilliseconds()} is a fixed nominal candle length for
 * every interval except {@code 1M}, where it is {@code null} because months
 * vary in length — Binance aligns those candles to the 1st-of-month UTC,
 * which is exactly what {@code plusMonths} on a real stored {@code open_time}
 * lands on.
 */
public final class TimeframeArithmetic {

    private TimeframeArithmetic() {
    }

    /** The {@code open_time} of the candle immediately after {@code openTime}. */
    public static OffsetDateTime advance(Timeframe timeframe, OffsetDateTime openTime) {
        Long millis = timeframe.getMilliseconds();
        return millis != null
                ? openTime.plus(Duration.ofMillis(millis))
                : openTime.plusMonths(1);
    }
}
