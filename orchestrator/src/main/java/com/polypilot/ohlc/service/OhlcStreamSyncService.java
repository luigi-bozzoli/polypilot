package com.polypilot.ohlc.service;

import com.polypilot.ohlc.client.BinanceClient;
import com.polypilot.ohlc.client.BinanceKline;
import com.polypilot.ohlc.config.BinanceProperties;
import com.polypilot.ohlc.repository.OhlcCandleRepository;
import com.polypilot.ohlc.support.TimeframeArithmetic;
import com.polypilot.reference.entity.Timeframe;
import com.polypilot.reference.service.ReferenceDataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * One-stream unit of work for {@link OhlcSyncService}. Lives in its own bean so
 * {@code REQUIRES_NEW} is a real proxy hop: a failure on one {@code (symbol,
 * timeframe)} rolls back only that stream and the batch loop moves on — the same
 * pattern as {@code MarketItemSyncService}.
 *
 * <p>Per run, per stream:
 * <ol>
 *   <li>cold start &rarr; backfill the last {@code backfill-candles} candles;
 *       otherwise fetch forward from the newest stored {@code open_time}
 *       (inclusive, so the previously-forming candle is finalized) and keep
 *       paging until the forming candle is reached;</li>
 *   <li>gap pass &rarr; find interior holes between stored candles and backfill
 *       them.</li>
 * </ol>
 * All writes go through {@link OhlcCandleRepository#upsert}, which keeps closed
 * candles immutable and overwrites only the forming one.
 */
@Slf4j
@Service
@AllArgsConstructor
public class OhlcStreamSyncService {

    private final BinanceClient binanceClient;
    private final OhlcCandleRepository ohlcCandleRepository;
    private final BinanceProperties binanceProperties;
    private final ReferenceDataService referenceDataService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncStream(String symbol, String timeframe) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Anchor the forward fetch at the oldest not-yet-closed candle so every
        // still-forming row is re-pulled and finalized — anchoring at max(open_time)
        // would let a new forming candle leapfrog one that never got its final values.
        int written = ohlcCandleRepository.findEarliestFormingOpenTime(symbol, timeframe)
                .or(() -> ohlcCandleRepository.findMaxOpenTime(symbol, timeframe))
                .map(anchor -> fetchForward(symbol, timeframe, anchor, now))
                .orElseGet(() -> backfill(symbol, timeframe, now));

        int gapWritten = fillGaps(symbol, timeframe, now);

        if (written + gapWritten > 0) {
            log.info("Synced [{} {}]: {} candle(s) forward, {} backfilled from gaps",
                    symbol, timeframe, written, gapWritten);
        } else {
            log.debug("Synced [{} {}]: nothing new", symbol, timeframe);
        }
    }

    /** Cold start: the most recent {@code backfill-candles} candles in one call. */
    private int backfill(String symbol, String timeframe, OffsetDateTime now) {
        List<BinanceKline> klines = binanceClient.fetchKlines(
                symbol, timeframe, null, null, binanceProperties.getBackfillCandles());
        return upsertAll(symbol, timeframe, klines, now);
    }

    /**
     * Fetch from {@code storedMax} forward. Binance {@code startTime} is
     * inclusive, so the first page re-returns the candle at {@code storedMax}
     * (finalizing it if it was forming) plus everything newer. Pages until the
     * forming candle is reached, bounding a long-idle stream's catch-up.
     */
    private int fetchForward(String symbol, String timeframe, OffsetDateTime storedMax, OffsetDateTime now) {
        int total = 0;
        OffsetDateTime cursor = storedMax;

        while (true) {
            long startMs = cursor.toInstant().toEpochMilli();
            List<BinanceKline> page = binanceClient.fetchKlines(
                    symbol, timeframe, startMs, null, BinanceClient.MAX_LIMIT);
            if (page.isEmpty()) {
                break;
            }

            total += upsertAll(symbol, timeframe, page, now);

            BinanceKline last = page.getLast();
            boolean reachedForming = !isClosed(last, now);
            if (reachedForming || page.size() < BinanceClient.MAX_LIMIT) {
                break;
            }

            OffsetDateTime next = last.getOpenTime().plus(Duration.ofMillis(1));
            if (!next.isAfter(cursor)) {
                break;  // no forward progress — stop rather than spin
            }
            cursor = next;
        }
        return total;
    }

    /**
     * Interior-hole detection: walk adjacent stored {@code open_time}s; wherever
     * the next stored candle is further out than one interval, backfill the span
     * in between. Never fetches earlier than the current stored minimum.
     */
    private int fillGaps(String symbol, String timeframe, OffsetDateTime now) {
        List<OffsetDateTime> times = ohlcCandleRepository.findOpenTimesAsc(symbol, timeframe);
        if (times.size() < 2) {
            return 0;
        }

        Timeframe timeframeEntity = referenceDataService.getTimeframe(timeframe);

        int total = 0;
        for (int i = 0; i < times.size() - 1; i++) {
            OffsetDateTime expectedNext = TimeframeArithmetic.advance(timeframeEntity, times.get(i));
            OffsetDateTime actualNext = times.get(i + 1);
            if (expectedNext.isBefore(actualNext)) {
                log.warn("Gap in [{} {}]: {} .. {} missing, backfilling",
                        symbol, timeframe, expectedNext, actualNext);
                total += backfillRange(symbol, timeframe, expectedNext, actualNext, now);
            }
        }
        return total;
    }

    /** Backfill candles with {@code from <= open_time < to}, paging as needed. */
    private int backfillRange(String symbol, String timeframe,
                              OffsetDateTime from, OffsetDateTime to, OffsetDateTime now) {
        int total = 0;
        OffsetDateTime cursor = from;
        long endMs = to.toInstant().toEpochMilli() - 1;  // endTime is inclusive

        while (cursor.isBefore(to)) {
            long startMs = cursor.toInstant().toEpochMilli();
            List<BinanceKline> page = binanceClient.fetchKlines(
                    symbol, timeframe, startMs, endMs, BinanceClient.MAX_LIMIT);
            if (page.isEmpty()) {
                break;
            }

            total += upsertAll(symbol, timeframe, page, now);

            OffsetDateTime next = page.getLast().getOpenTime().plus(Duration.ofMillis(1));
            if (page.size() < BinanceClient.MAX_LIMIT || !next.isAfter(cursor)) {
                break;
            }
            cursor = next;
        }
        return total;
    }

    private int upsertAll(String symbol, String timeframe, List<BinanceKline> klines, OffsetDateTime now) {
        for (BinanceKline k : klines) {
            ohlcCandleRepository.upsert(symbol, timeframe, k.getOpenTime(),
                    k.getOpen(), k.getHigh(), k.getLow(), k.getClose(), k.getVolume(),
                    k.getCloseTime(), isClosed(k, now));
        }
        return klines.size();
    }

    private static boolean isClosed(BinanceKline kline, OffsetDateTime now) {
        // `now` is captured before the HTTP call, so a candle whose close_time is
        // already past is guaranteed to have been final in Binance's response.
        return kline.getCloseTime().isBefore(now);
    }
}
