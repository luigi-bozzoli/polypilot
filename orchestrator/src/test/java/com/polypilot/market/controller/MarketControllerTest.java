package com.polypilot.market.controller;

import com.polypilot.ai.service.NewsQueryService;
import com.polypilot.ai.service.SentimentQueryService;
import com.polypilot.audit.service.AuditLogQueryService;
import com.polypilot.enums.order.OrderStatus;
import com.polypilot.market.dto.view.EngineDecisionView;
import com.polypilot.market.dto.view.MarketDecisionsView;
import com.polypilot.market.dto.view.MarketOhlcView;
import com.polypilot.market.dto.view.MarketOrderView;
import com.polypilot.market.dto.view.MarketOrdersView;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.service.MarketOhlcService;
import com.polypilot.market.service.MarketPriceHistoryService;
import com.polypilot.market.service.MarketSyncService;
import com.polypilot.market.service.SeriesQueryService;
import com.polypilot.trading.OrderQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test on the controller directly — no {@code @WebMvcTest} slice, matching
 * {@code StrategyControllerTest}'s convention. Covers {@code GET /market/{marketId}/decisions}
 * and {@code GET /market/{marketId}/ohlc}; the sync/series/price-history routes have no prior
 * test file and are out of scope for this change.
 */
class MarketControllerTest {

    private final MarketSyncService marketSyncService = Mockito.mock(MarketSyncService.class);
    private final SeriesQueryService seriesQueryService = Mockito.mock(SeriesQueryService.class);
    private final MarketPriceHistoryService marketPriceHistoryService = Mockito.mock(MarketPriceHistoryService.class);
    private final MarketOhlcService marketOhlcService = Mockito.mock(MarketOhlcService.class);
    private final AuditLogQueryService auditLogQueryService = Mockito.mock(AuditLogQueryService.class);
    private final NewsQueryService newsQueryService = Mockito.mock(NewsQueryService.class);
    private final SentimentQueryService sentimentQueryService = Mockito.mock(SentimentQueryService.class);
    private final OrderQueryService orderQueryService = Mockito.mock(OrderQueryService.class);
    private final MarketController controller = new MarketController(
            marketSyncService, seriesQueryService, marketPriceHistoryService, marketOhlcService,
            auditLogQueryService, newsQueryService, sentimentQueryService, orderQueryService);
    private final UUID userId = UUID.randomUUID();

    @Test
    void getDecisions_delegatesToAuditLogQueryServiceWithLimit() {
        EngineDecisionView decision = EngineDecisionView.builder()
                .id(UUID.randomUUID()).at(OffsetDateTime.now()).kind("DRY_RUN_ORDER")
                .detail("simulated 50 YES @ 0.71").build();
        MarketDecisionsView expected = MarketDecisionsView.builder()
                .marketId("m-1").decisions(List.of(decision)).build();
        when(auditLogQueryService.getMarketDecisions(userId, "m-1", 20)).thenReturn(expected);

        MarketDecisionsView result = controller.getDecisions("m-1", 20, userId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getOhlc_delegatesToMarketOhlcServiceWithTimeframeAndLimit() {
        MarketOhlcView expected = MarketOhlcView.builder()
                .marketId("m-1").symbol("BTCUSDT").timeframe("1h").candles(List.of()).build();
        when(marketOhlcService.getOhlc("m-1", "1h", 200)).thenReturn(expected);

        MarketOhlcView result = controller.getOhlc("m-1", "1h", 200);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getOrders_delegatesToOrderQueryServiceWithLimit() {
        MarketOrderView order = MarketOrderView.builder()
                .id(UUID.randomUUID()).placedAt(OffsetDateTime.now())
                .strategyId(UUID.randomUUID()).strategyName("BTC Bullish Momentum")
                .tokenSide(MarketOutcome.YES)
                .sizeRequested(BigDecimal.valueOf(50)).sizeFilled(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(0.71)).status(OrderStatus.FILLED).isDryRun(true)
                .build();
        MarketOrdersView expected = MarketOrdersView.builder().marketId("m-1").orders(List.of(order)).build();
        when(orderQueryService.getMarketOrders(userId, "m-1", 20)).thenReturn(expected);

        MarketOrdersView result = controller.getOrders("m-1", 20, userId);

        assertThat(result).isEqualTo(expected);
    }
}
