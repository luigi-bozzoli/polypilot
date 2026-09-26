package com.polypilot.reference.service;

import com.polypilot.common.cache.JsonRedisCache;
import com.polypilot.reference.dto.view.TimeframeView;
import com.polypilot.reference.entity.Ticker;
import com.polypilot.reference.entity.Timeframe;
import com.polypilot.reference.repository.TickerRepository;
import com.polypilot.reference.repository.TimeframeRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

/**
 * Read access to the DB-backed {@code ticker} / {@code timeframe} reference
 * tables. Replaces the old {@code binance.symbols} / {@code binance.timeframes}
 * yaml config: the {@code ohlc-sync} job reads the enabled rows here on every
 * run, so a symbol or interval can be added/disabled with a single UPDATE and no
 * redeploy.
 *
 * <p>{@link #getTimeframe(String)} is read-through cached in Redis (via the
 * shared {@link JsonRedisCache}) under {@code timeframe:v1:code:*} with a fixed
 * TTL ({@code timeframe.cache-ttl}, default 24h) — the row is managed directly
 * in the DB with no CRUD API, so a long TTL is fine and a manual edit just takes
 * up to that long to be picked up.
 *
 * <p>Everything else here stays uncached — both tables hold a handful of rows
 * and {@link #enabledBinanceSymbols()} / {@link #enabledTimeframeCodes()} are
 * each read once per minute by the {@code ohlc-sync} job.
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    static final String CACHE_KEY_PREFIX = "timeframe:v1:code:";

    private final TickerRepository tickerRepository;
    private final TimeframeRepository timeframeRepository;
    private final JsonRedisCache cache;
    @Value("${timeframe.cache-ttl:24h}")
    private Duration cacheTtl;

    /** Binance Spot pairs (e.g. {@code BTCUSDT}) for every enabled ticker, in sort order. */
    @Transactional(readOnly = true)
    public List<String> enabledBinanceSymbols() {
        return tickerRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(Ticker::getBinanceSymbol)
                .toList();
    }

    /** Binance interval codes (e.g. {@code 1h}) for every enabled timeframe, in sort order. */
    @Transactional(readOnly = true)
    public List<String> enabledTimeframeCodes() {
        return timeframeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(Timeframe::getCode)
                .toList();
    }

    /** Enabled timeframes in display order, for the dashboard's chart interval selector. */
    @Transactional(readOnly = true)
    public List<TimeframeView> listEnabledTimeframes() {
        return timeframeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(t -> TimeframeView.builder().code(t.getCode()).label(t.getLabel()).build())
                .toList();
    }

    /** The {@link Timeframe} row for a Binance interval code (e.g. {@code 1h}). */
    @Transactional(readOnly = true)
    public Timeframe getTimeframe(String code) {
        String cacheKey = CACHE_KEY_PREFIX + code;

        Timeframe cached = cache.get(cacheKey, Timeframe.class);
        if (cached != null) {
            return cached;
        }

        Timeframe timeframe = timeframeRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown timeframe: " + code));

        cache.put(cacheKey, timeframe, cacheTtl);
        return timeframe;
    }
}
