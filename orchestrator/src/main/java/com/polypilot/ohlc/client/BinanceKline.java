package com.polypilot.ohlc.client;

import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * One kline (candlestick) from {@code GET /api/v3/klines}. Binance returns each
 * kline as a JSON array, not an object:
 *
 * <pre>
 *   [ 1499040000000,      // 0  open time (ms)
 *     "0.01634790",       // 1  open
 *     "0.80000000",       // 2  high
 *     "0.01575800",       // 3  low
 *     "0.01577100",       // 4  close
 *     "148976.11427815",  // 5  base-asset volume
 *     1499644799999,      // 6  close time (ms)
 *     ... ]               // 7-11 quote volume, trades, taker buys, ignore
 * </pre>
 *
 * Only indices 0-6 are kept.
 */
@Value
public class BinanceKline {

    OffsetDateTime openTime;
    BigDecimal open;
    BigDecimal high;
    BigDecimal low;
    BigDecimal close;
    BigDecimal volume;
    OffsetDateTime closeTime;

    /** True once the interval has ended relative to {@code now}. */
    public boolean isClosed(OffsetDateTime now) {
        return closeTime.isBefore(now);
    }

    public static BinanceKline fromArray(List<?> row) {
        return new BinanceKline(
                epochMillisToUtc(row.get(0)),
                toBigDecimal(row.get(1)),
                toBigDecimal(row.get(2)),
                toBigDecimal(row.get(3)),
                toBigDecimal(row.get(4)),
                toBigDecimal(row.get(5)),
                epochMillisToUtc(row.get(6)));
    }

    private static BigDecimal toBigDecimal(Object raw) {
        return new BigDecimal(String.valueOf(raw));
    }

    private static OffsetDateTime epochMillisToUtc(Object raw) {
        long millis = ((Number) raw).longValue();
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }
}
