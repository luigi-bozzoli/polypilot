package com.polypilot.market.service;

import com.polypilot.market.dto.view.MarketOhlcView;
import com.polypilot.market.dto.view.OhlcCandleView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.Series;
import com.polypilot.market.mapper.MarketViewMapper;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.ohlc.entity.OhlcCandle;
import com.polypilot.ohlc.repository.OhlcCandleRepository;
import com.polypilot.reference.entity.Ticker;
import com.polypilot.reference.entity.Timeframe;
import com.polypilot.reference.repository.TimeframeRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-side query for a market's OHLC candlestick chart, backing the dashboard's Market
 * Details page. Contract: {@code contracts/market-ohlc.md}.
 *
 * <p>Unlike {@link MarketPriceHistoryService} (which reads {@code price_snapshots}, keyed by
 * {@code market_id}), the candle data here lives in {@code ohlc_candles}, keyed by
 * {@code (symbol, timeframe, open_time)} — a Binance asset stream with no relation to a
 * Polymarket market. The bridge is {@code market.series.asset.binanceSymbol}, the same
 * resolution {@code StrategyService.runEvaluation} already does for strategy evaluation.
 * {@code asset} is nullable and not backfilled for every series (see {@link Series#getAsset()}),
 * so "no linked asset" is a normal, expected outcome here — not an error.
 */
@Service
@AllArgsConstructor
public class MarketOhlcService {

    static final String DEFAULT_TIMEFRAME = "1h";
    static final int DEFAULT_LIMIT = 200;
    static final int MAX_LIMIT = 1000;

    private final MarketRepository marketRepository;
    private final OhlcCandleRepository ohlcCandleRepository;
    private final TimeframeRepository timeframeRepository;
    private final MarketViewMapper marketViewMapper;

    @Transactional(readOnly = true)
    public MarketOhlcView getOhlc(String marketId, String timeframeParam, Integer limitParam) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Market not found: " + marketId));

        String timeframe = resolveTimeframe(timeframeParam);
        int limit = clampLimit(limitParam);

        Series series = market.getSeries();
        Ticker asset = series != null ? series.getAsset() : null;

        if (asset == null) {
            return MarketOhlcView.builder()
                    .marketId(marketId)
                    .symbol(null)
                    .timeframe(timeframe)
                    .candles(Collections.emptyList())
                    .build();
        }

        List<OhlcCandle> candlesDesc = ohlcCandleRepository
                .findRecentCandles(asset.getBinanceSymbol(), timeframe, PageRequest.of(0, limit));
        List<OhlcCandle> candlesAsc = new ArrayList<>(candlesDesc);
        Collections.reverse(candlesAsc);

        List<OhlcCandleView> candles = candlesAsc.stream()
                .map(marketViewMapper::toCandleView)
                .toList();

        return MarketOhlcView.builder()
                .marketId(marketId)
                .symbol(asset.getBinanceSymbol())
                .timeframe(timeframe)
                .candles(candles)
                .build();
    }

    /** Explicit unknown/disabled codes 400; an omitted param falls back to {@link #DEFAULT_TIMEFRAME}. */
    private String resolveTimeframe(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TIMEFRAME;
        }
        Timeframe timeframe = timeframeRepository.findById(raw)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown timeframe: " + raw));
        if (!timeframe.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timeframe not enabled: " + raw);
        }
        return timeframe.getCode();
    }

    private int clampLimit(Integer raw) {
        if (raw == null || raw < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(raw, MAX_LIMIT);
    }
}
