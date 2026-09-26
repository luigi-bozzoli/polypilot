package com.polypilot.strategy.controller;

import com.polypilot.audit.service.AuditLogQueryService;
import com.polypilot.enums.order.OrderStatus;
import com.polypilot.enums.order.OrderType;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.strategy.MarketFieldCompareNode;
import com.polypilot.strategy.dto.request.CreateStrategyRequest;
import com.polypilot.strategy.dto.request.SetStrategyEnabledRequest;
import com.polypilot.strategy.dto.view.ConditionFieldsCatalogView;
import com.polypilot.strategy.dto.view.IndicatorConditionFieldView;
import com.polypilot.strategy.dto.view.MarketFieldCatalogEntryView;
import com.polypilot.strategy.dto.view.StrategyDecisionView;
import com.polypilot.strategy.dto.view.StrategyOrderView;
import com.polypilot.strategy.dto.view.StrategyView;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.MarketFieldDataType;
import com.polypilot.strategy.enums.MarketFieldValueScale;
import com.polypilot.strategy.service.StrategyConditionFieldsService;
import com.polypilot.strategy.service.StrategyService;
import com.polypilot.trading.OrderQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test on the controller directly — no {@code @WebMvcTest} slice, matching the rest of this
 * codebase's test conventions (no controller-test harness exists yet, and
 * {@code spring-boot-webmvc-test} isn't a transitive dependency of {@code spring-boot-starter-test}
 * on this pom). The 200-status / routing / auth-filter behavior is Spring MVC's own concern, not
 * this method's; what's verified here is the response body's shape and delegation.
 */
class StrategyControllerTest {

    private final StrategyConditionFieldsService conditionFieldsService = Mockito.mock(StrategyConditionFieldsService.class);
    private final StrategyService strategyService = Mockito.mock(StrategyService.class);
    private final AuditLogQueryService auditLogQueryService = Mockito.mock(AuditLogQueryService.class);
    private final OrderQueryService orderQueryService = Mockito.mock(OrderQueryService.class);
    private final StrategyController controller =
            new StrategyController(conditionFieldsService, strategyService, auditLogQueryService, orderQueryService);
    private final UUID userId = UUID.randomUUID();

    @Test
    void getConditionFields_returnsBothMarketFieldsAndIndicators() {
        when(conditionFieldsService.getConditionFields()).thenReturn(realisticCatalog());

        ConditionFieldsCatalogView body = controller.getConditionFields();

        assertThat(body.getMarketFields()).hasSize(6);
        assertThat(body.getMarketFields()).extracting(MarketFieldCatalogEntryView::getField)
                .containsExactly(
                        MarketField.UP_PRICE, MarketField.DOWN_PRICE, MarketField.VOLUME_24H,
                        MarketField.LIQUIDITY, MarketField.SENTIMENT, MarketField.SENTIMENT_CONFIDENCE);

        MarketFieldCatalogEntryView upPrice = body.getMarketFields().get(0);
        assertThat(upPrice.getDataType()).isEqualTo(MarketFieldDataType.NUMBER);
        assertThat(upPrice.getValueScale()).isEqualTo(MarketFieldValueScale.OSCILLATOR_0_1);

        MarketFieldCatalogEntryView sentiment = body.getMarketFields().get(4);
        assertThat(sentiment.getDataType()).isEqualTo(MarketFieldDataType.ENUM);
        assertThat(sentiment.getAllowedValues()).containsExactly("BULLISH", "NEUTRAL", "BEARISH");
        assertThat(sentiment.getOperators()).containsExactly(CompareOperator.EQ, CompareOperator.NEQ);

        assertThat(body.getIndicators()).hasSize(1);
        assertThat(body.getIndicators().get(0).getIndicatorKey()).isEqualTo("rsi");
        assertThat(body.getIndicators().get(0).getOperators()).containsExactly(CompareOperator.values());
    }

    @Test
    void listStrategies_delegatesToQueryService() {
        StrategyView strategy = strategyView(UUID.randomUUID(), "btc-dip-buy");
        when(strategyService.listStrategies(userId)).thenReturn(List.of(strategy));

        List<StrategyView> result = controller.listStrategies(userId);

        assertThat(result).containsExactly(strategy);
    }

    @Test
    void getStrategy_delegatesToQueryServiceById() {
        UUID id = UUID.randomUUID();
        StrategyView strategy = strategyView(id, "btc-dip-buy");
        when(strategyService.getStrategy(id, userId)).thenReturn(strategy);

        StrategyView result = controller.getStrategy(id, userId);

        assertThat(result).isEqualTo(strategy);
    }

    @Test
    void createStrategy_returns201WithLocationAndTheCreatedView() {
        CreateStrategyRequest request = CreateStrategyRequest.builder()
                .name("btc-dip-buy")
                .tokenSide(MarketOutcome.YES)
                .orderType(OrderType.GTC)
                .ruleTree(new MarketFieldCompareNode(MarketField.UP_PRICE, CompareOperator.LT, BigDecimal.valueOf(0.5), null))
                .maxBetSize(BigDecimal.TEN)
                .maxDailyExposure(BigDecimal.valueOf(100))
                .cronExpression("0 */5 * * * *")
                .seriesId(UUID.randomUUID())
                .dryRun(true)
                .build();
        UUID id = UUID.randomUUID();
        StrategyView created = strategyView(id, "btc-dip-buy");
        when(strategyService.create(request, userId)).thenReturn(created);

        ResponseEntity<StrategyView> response = controller.createStrategy(request, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasPath("/strategies/" + id);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void setEnabled_delegatesToServiceWithTheRequestedFlag() {
        UUID id = UUID.randomUUID();
        SetStrategyEnabledRequest request = SetStrategyEnabledRequest.builder().enabled(true).build();
        StrategyView updated = strategyView(id, "btc-dip-buy");
        when(strategyService.setEnabled(id, userId, true)).thenReturn(updated);

        StrategyView result = controller.setEnabled(id, request, userId);

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void deleteStrategy_delegatesToDeleteServiceAndReturns204() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteStrategy(id, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Mockito.verify(strategyService).delete(id, userId);
    }

    @Test
    void getDecisions_delegatesToAuditLogQueryServiceWithLimit() {
        UUID id = UUID.randomUUID();
        StrategyDecisionView decision = StrategyDecisionView.builder()
                .kind("DRY_RUN_ORDER").detail("simulated 50 YES @ 0.71").auditLogId(UUID.randomUUID())
                .build();
        when(auditLogQueryService.getStrategyDecisions(userId, id, 20)).thenReturn(List.of(decision));

        List<StrategyDecisionView> result = controller.getDecisions(id, 20, userId);

        assertThat(result).containsExactly(decision);
    }

    @Test
    void getOrders_delegatesToOrderQueryServiceWithLimit() {
        UUID id = UUID.randomUUID();
        StrategyOrderView order = StrategyOrderView.builder()
                .id(UUID.randomUUID())
                .marketId("0x4f2a9c")
                .marketQuestion("Will BTC close above $110K?")
                .tokenSide(MarketOutcome.YES)
                .sizeRequested(BigDecimal.valueOf(50))
                .sizeFilled(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(0.71))
                .status(OrderStatus.FILLED)
                .isDryRun(true)
                .build();
        when(orderQueryService.getStrategyOrders(userId, id, 20)).thenReturn(List.of(order));

        List<StrategyOrderView> result = controller.getOrders(id, 20, userId);

        assertThat(result).containsExactly(order);
    }

    private StrategyView strategyView(UUID id, String name) {
        return StrategyView.builder()
                .id(id)
                .name(name)
                .enabled(false)
                .dryRun(true)
                .cronExpression("0 */5 * * * *")
                .seriesId(UUID.randomUUID())
                .tokenSide(MarketOutcome.UP)
                .orderType(OrderType.GTC)
                .maxBetSize(BigDecimal.TEN)
                .maxDailyExposure(BigDecimal.valueOf(100))
                .build();
    }

    private ConditionFieldsCatalogView realisticCatalog() {
        return ConditionFieldsCatalogView.builder()
                .marketFields(List.of(
                        MarketFieldCatalogEntryView.builder()
                                .field(MarketField.UP_PRICE).name("YES probability")
                                .dataType(MarketFieldDataType.NUMBER).valueScale(MarketFieldValueScale.OSCILLATOR_0_1)
                                .operators(List.of(CompareOperator.LT, CompareOperator.GT, CompareOperator.EQ))
                                .build(),
                        MarketFieldCatalogEntryView.builder()
                                .field(MarketField.DOWN_PRICE).name("NO probability")
                                .dataType(MarketFieldDataType.NUMBER).valueScale(MarketFieldValueScale.OSCILLATOR_0_1)
                                .operators(List.of(CompareOperator.LT, CompareOperator.GT, CompareOperator.EQ))
                                .build(),
                        MarketFieldCatalogEntryView.builder()
                                .field(MarketField.VOLUME_24H).name("24h volume")
                                .dataType(MarketFieldDataType.NUMBER).valueScale(MarketFieldValueScale.VOLUME)
                                .operators(List.of(CompareOperator.LT, CompareOperator.GT))
                                .build(),
                        MarketFieldCatalogEntryView.builder()
                                .field(MarketField.LIQUIDITY).name("Liquidity")
                                .dataType(MarketFieldDataType.NUMBER).valueScale(MarketFieldValueScale.VOLUME)
                                .operators(List.of(CompareOperator.LT, CompareOperator.GT))
                                .build(),
                        MarketFieldCatalogEntryView.builder()
                                .field(MarketField.SENTIMENT).name("AI sentiment is")
                                .dataType(MarketFieldDataType.ENUM)
                                .allowedValues(List.of("BULLISH", "NEUTRAL", "BEARISH"))
                                .operators(List.of(CompareOperator.EQ, CompareOperator.NEQ))
                                .build(),
                        MarketFieldCatalogEntryView.builder()
                                .field(MarketField.SENTIMENT_CONFIDENCE).name("Sentiment confidence")
                                .dataType(MarketFieldDataType.NUMBER).valueScale(MarketFieldValueScale.OSCILLATOR_0_1)
                                .operators(List.of(CompareOperator.LT, CompareOperator.GT))
                                .build()))
                .indicators(List.of(IndicatorConditionFieldView.builder()
                        .indicatorKey("rsi")
                        .name("Relative Strength Index")
                        .abbreviation("RSI")
                        .operators(List.of(CompareOperator.values()))
                        .build()))
                .build();
    }
}
