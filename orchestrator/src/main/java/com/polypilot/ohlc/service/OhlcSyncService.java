package com.polypilot.ohlc.service;

import com.polypilot.reference.service.ReferenceDataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batch driver for the {@code ohlc-sync} job. Iterates every configured
 * {@code (symbol, timeframe)} stream and delegates each to
 * {@link OhlcStreamSyncService}, which runs it in its own {@code REQUIRES_NEW}
 * transaction — one stream failing is logged and skipped, the rest continue.
 * Mirrors {@code MarketSyncService}.
 *
 * <p>Streams run sequentially on the scheduler thread; at ~1 call/stream/run
 * this stays far under Binance's IP weight budget, so there is no fan-out and no
 * rate limiter.
 *
 * <p>The set of streams comes from the DB-backed {@code ticker} / {@code timeframe}
 * reference tables via {@link ReferenceDataService} (enabled rows only), not from
 * config — add or disable a row to change what is synced, no redeploy.
 */
@Slf4j
@Service
@AllArgsConstructor
public class OhlcSyncService {

    private final ReferenceDataService referenceData;
    private final OhlcStreamSyncService streamSyncService;

    public void syncAll() {
        List<String> symbols = referenceData.enabledBinanceSymbols();
        List<String> timeframes = referenceData.enabledTimeframeCodes();
        if (symbols.isEmpty() || timeframes.isEmpty()) {
            log.warn("OHLC sync skipped: no enabled ticker(s)/timeframe(s) configured");
            return;
        }
        log.info("OHLC sync starting: {} symbol(s) x {} timeframe(s)", symbols.size(), timeframes.size());

        int ok = 0;
        int failed = 0;
        for (String symbol : symbols) {
            for (String timeframe : timeframes) {
                try {
                    streamSyncService.syncStream(symbol, timeframe);
                    ok++;
                } catch (Exception ex) {
                    failed++;
                    log.error("Failed to sync OHLC stream [{} {}], skipping", symbol, timeframe, ex);
                }
            }
        }
        log.info("OHLC sync finished: {} ok, {} failed", ok, failed);
    }
}
