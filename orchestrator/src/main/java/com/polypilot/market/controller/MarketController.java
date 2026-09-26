package com.polypilot.market.controller;


import com.polypilot.ai.dto.view.MarketNewsSummaryView;
import com.polypilot.ai.dto.view.MarketSentimentView;
import com.polypilot.ai.service.NewsQueryService;
import com.polypilot.ai.service.SentimentQueryService;
import com.polypilot.audit.service.AuditLogQueryService;
import com.polypilot.market.dto.view.MarketDecisionsView;
import com.polypilot.market.dto.view.MarketOhlcView;
import com.polypilot.market.dto.view.MarketOrdersView;
import com.polypilot.market.dto.view.MarketPriceHistoryView;
import com.polypilot.market.dto.view.SeriesDetailView;
import com.polypilot.market.dto.view.SeriesSummaryView;
import com.polypilot.market.service.MarketOhlcService;
import com.polypilot.market.service.MarketPriceHistoryService;
import com.polypilot.market.service.MarketSyncService;
import com.polypilot.market.service.SeriesQueryService;
import com.polypilot.trading.OrderQueryService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/market")
@AllArgsConstructor
public class MarketController {

    private final MarketSyncService marketSyncService;
    private final SeriesQueryService seriesQueryService;
    private final MarketPriceHistoryService marketPriceHistoryService;
    private final MarketOhlcService marketOhlcService;
    private final AuditLogQueryService auditLogQueryService;
    private final NewsQueryService newsQueryService;
    private final SentimentQueryService sentimentQueryService;
    private final OrderQueryService orderQueryService;

    /**
     * Manual trigger, e.g. for ops/debugging. The real sync now runs on a
     * schedule via MarketScheduler, so this is optional to keep.
     */
    @PostMapping
    public String syncMarketsNow() {
        marketSyncService.syncAllSeries();
        return "Series/market sync triggered";
    }

    /** Series list for the dashboard, each with its current market and counts. */
    @GetMapping("/series")
    public List<SeriesSummaryView> listSeries() {
        return seriesQueryService.listSeries();
    }

    /** One series with every market that belongs to it. */
    @GetMapping("/series/{id}")
    public SeriesDetailView getSeries(@PathVariable("id") UUID id) {
        return seriesQueryService.getSeriesDetail(id);
    }

    /**
     * Probability / price history for one market, oldest-first, backing the
     * dashboard chart. {@code window} is a look-back hint (12h, 24h, 7d, …);
     * the server may clamp it and echoes the effective value.
     * Contract: {@code contracts/market-price-history.md}.
     */
    @GetMapping("/{marketId}/price-history")
    public MarketPriceHistoryView getPriceHistory(
            @PathVariable("marketId") String marketId,
            @RequestParam(name = "window", defaultValue = "12h") String window) {
        return marketPriceHistoryService.getPriceHistory(marketId, window);
    }

    /**
     * OHLC candles for one market's underlying asset (via its series' linked ticker),
     * oldest-first, backing the dashboard's candlestick chart. {@code timeframe} defaults to
     * "1h" if omitted; an explicit unknown/disabled code is rejected. {@code symbol} in the
     * response is null when the series has no linked asset — a normal, non-error state.
     * Contract: {@code contracts/market-ohlc.md}.
     */
    @GetMapping("/{marketId}/ohlc")
    public MarketOhlcView getOhlc(
            @PathVariable("marketId") String marketId,
            @RequestParam(name = "timeframe", required = false) String timeframe,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return marketOhlcService.getOhlc(marketId, timeframe, limit);
    }

    /**
     * Market-scoped slice of the audit feed, newest first. Contract:
     * {@code contracts/market-engine-decisions.md}.
     */
    @GetMapping("/{marketId}/decisions")
    public MarketDecisionsView getDecisions(
            @PathVariable("marketId") String marketId,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return auditLogQueryService.getMarketDecisions(userId, marketId, limit);
    }

    /**
     * Market-scoped slice of {@code orders}, newest first. Contract:
     * {@code contracts/market-recent-orders.md}.
     */
    @GetMapping("/{marketId}/orders")
    public MarketOrdersView getOrders(
            @PathVariable("marketId") String marketId,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return orderQueryService.getMarketOrders(userId, marketId, limit);
    }

    /**
     * Latest non-expired news summary. Contract: {@code contracts/market-news-summary.md}.
     * {@code news_summaries} is market-scoped, not user-scoped (same as price history) —
     * no {@code @AuthenticationPrincipal} needed.
     */
    @GetMapping("/{marketId}/news/latest")
    public ResponseEntity<MarketNewsSummaryView> getLatestNews(@PathVariable("marketId") String marketId) {
        return newsQueryService.getLatest(marketId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Latest sentiment score, any age (no expiry concept for sentiment). Contract:
     * {@code contracts/market-sentiment.md}.
     */
    @GetMapping("/{marketId}/sentiment/latest")
    public ResponseEntity<MarketSentimentView> getLatestSentiment(@PathVariable("marketId") String marketId) {
        return sentimentQueryService.getLatest(marketId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
