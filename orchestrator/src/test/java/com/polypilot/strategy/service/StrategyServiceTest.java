package com.polypilot.strategy.service;

import com.polypilot.entity.AuditLog;
import com.polypilot.entity.Order;
import com.polypilot.entity.Position;
import com.polypilot.entity.Strategy;
import com.polypilot.enums.alert.ActionType;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.Series;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.repository.SeriesRepository;
import com.polypilot.reference.entity.Ticker;
import com.polypilot.repository.AuditLogRepository;
import com.polypilot.repository.OrderRepository;
import com.polypilot.strategy.StrategyEvaluationService;
import com.polypilot.strategy.json.StrategyNodeJacksonModule;
import com.polypilot.strategy.mapper.RuleTreeMapper;
import com.polypilot.strategy.mapper.StrategyViewMapper;
import com.polypilot.strategy.repository.StrategyRepository;
import com.polypilot.strategy.scheduler.StrategyScheduler;
import com.polypilot.trading.OpenTradeResult;
import com.polypilot.trading.OpenTradeService;
import com.polypilot.strategy.dto.view.StrategyView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link StrategyService#runEvaluation} — the per-tick evaluation-and-audit unit of
 * work, moved here from {@code StrategyEvaluationRunner} so its {@code REQUIRES_NEW} transaction
 * is a real proxy hop instead of a self-invoked call. The lock/transaction wrapper itself is an
 * infrastructure concern exercised by {@code StrategyEvaluationRunnerTest}, not here.
 *
 * <p>A {@code true} evaluation result also attempts to open a
 * (simulated) trade via {@link OpenTradeService} and writes a second {@code audit_logs} row for
 * that outcome — see {@code evaluatesTrue_*} below.
 *
 * <p>A strategy is attached to a series, not a fixed market — {@code runEvaluation} resolves the
 * evaluation target (the series's current OPEN market + linked asset's Binance symbol) fresh on
 * every tick via {@code marketRepository}, rather than reading it off the entity.
 */
@ExtendWith(MockitoExtension.class)
class StrategyServiceTest {

    private static final String MARKET_ID = "market-1";
    private static final String BINANCE_SYMBOL = "BTCUSDT";

    @Mock
    private StrategyRepository strategyRepository;
    @Mock
    private StrategyViewMapper strategyViewMapper;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private StrategyRequestValidator strategyRequestValidator;
    @Mock
    private StrategyScheduler strategyScheduler;
    @Mock
    private StrategyEvaluationService strategyEvaluationService;
    @Mock
    private OpenTradeService openTradeService;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private MarketRepository marketRepository;

    private final RuleTreeMapper ruleTreeMapper = new RuleTreeMapper(
            JsonMapper.builder().addModule(new StrategyNodeJacksonModule().strategyNodeModule()).build());

    private StrategyService strategyService;

    @BeforeEach
    void setUp() {
        strategyService = new StrategyService(
                strategyRepository, strategyViewMapper, orderRepository, auditLogRepository,
                ruleTreeMapper, strategyRequestValidator, strategyScheduler, strategyEvaluationService,
                openTradeService, JsonMapper.builder().build(), seriesRepository, marketRepository);
    }

    @Test
    void skipsWhenStrategyNoLongerExists() {
        UUID id = UUID.randomUUID();
        when(strategyRepository.findById(id)).thenReturn(Optional.empty());

        strategyService.runEvaluation(id);

        verifyNoInteractions(strategyEvaluationService, auditLogRepository, openTradeService);
    }

    @Test
    void skipsWhenStrategyIsDisabled() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(false));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));

        strategyService.runEvaluation(strategy.getId());

        verifyNoInteractions(strategyEvaluationService, auditLogRepository, openTradeService);
    }

    @Test
    void skipsWhenStrategyIsSoftDeleted() {
        Strategy strategy = strategy(UUID.randomUUID(),
                builder -> builder.enabled(true).deletedAt(OffsetDateTime.now()));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));

        strategyService.runEvaluation(strategy.getId());

        verifyNoInteractions(strategyEvaluationService, auditLogRepository, openTradeService);
    }

    @Test
    void skipsWithoutWritingAuditLogWhenSeriesHasNoOpenMarket() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(true));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        // marketRepository left unstubbed -> Optional.empty() -> unresolvable marketId

        strategyService.runEvaluation(strategy.getId());

        verifyNoInteractions(strategyEvaluationService, auditLogRepository, openTradeService);
    }

    @Test
    void skipsWithoutWritingAuditLogWhenSeriesHasNoLinkedAsset() {
        Series seriesWithoutAsset = Series.builder().id(UUID.randomUUID()).asset(null).build();
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(true).series(seriesWithoutAsset));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));

        strategyService.runEvaluation(strategy.getId());

        verifyNoInteractions(strategyEvaluationService, auditLogRepository, openTradeService);
    }

    @Test
    void writesFailedAuditLogWhenRuleTreeFailsToParse() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder
                .enabled(true).ruleTree("{not valid json"));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        stubOpenMarket(strategy, MARKET_ID);

        strategyService.runEvaluation(strategy.getId());

        verifyNoInteractions(strategyEvaluationService, openTradeService);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ActionType.STRATEGY_EVALUATED);
        assertThat(captor.getValue().getStrategyId()).isEqualTo(strategy.getId());
        assertThat(captor.getValue().getReasoning()).contains("Rule tree failed to parse");
    }

    @Test
    void evaluatesFalse_writesOnlyTheEvaluationAuditLog_andNeverAttemptsATrade() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(true));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        stubOpenMarket(strategy, MARKET_ID);
        when(strategyEvaluationService.evaluate(any(), eq(BINANCE_SYMBOL), eq(MARKET_ID))).thenReturn(false);

        strategyService.runEvaluation(strategy.getId());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(ActionType.STRATEGY_EVALUATED);
        assertThat(saved.getMarketId()).isEqualTo(MARKET_ID);
        assertThat(saved.getUserId()).isEqualTo(strategy.getUserId());
        assertThat(saved.getIsDryRun()).isEqualTo(strategy.getDryRun());
        assertThat(saved.getSignals()).contains("\"result\":false");
        verifyNoInteractions(openTradeService);
    }

    @Test
    void evaluatesTrue_writesTheEvaluationAuditLog_thenAttemptsATradeAndWritesASecondAuditLog() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(true));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        stubOpenMarket(strategy, MARKET_ID);
        when(strategyEvaluationService.evaluate(any(), eq(BINANCE_SYMBOL), eq(MARKET_ID))).thenReturn(true);

        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).build();
        OpenTradeResult tradeResult = OpenTradeResult.opened(order, Position.builder().build(),
                "Filled 20 @ 0.5 (YES)");
        when(openTradeService.openTrade(strategy, MARKET_ID)).thenReturn(tradeResult);

        strategyService.runEvaluation(strategy.getId());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(captor.capture());
        List<AuditLog> saved = captor.getAllValues();

        AuditLog evaluatedRow = saved.get(0);
        assertThat(evaluatedRow.getAction()).isEqualTo(ActionType.STRATEGY_EVALUATED);
        assertThat(evaluatedRow.getMarketId()).isEqualTo(MARKET_ID);
        assertThat(evaluatedRow.getUserId()).isEqualTo(strategy.getUserId());
        assertThat(evaluatedRow.getIsDryRun()).isEqualTo(strategy.getDryRun());
        assertThat(evaluatedRow.getSignals()).contains("\"result\":true");

        AuditLog tradeRow = saved.get(1);
        assertThat(tradeRow.getAction()).isEqualTo(ActionType.DRY_RUN_ORDER);
        assertThat(tradeRow.getOrderId()).isEqualTo(orderId);
        assertThat(tradeRow.getMarketId()).isEqualTo(MARKET_ID);
        assertThat(tradeRow.getUserId()).isEqualTo(strategy.getUserId());
        assertThat(tradeRow.getIsDryRun()).isTrue();
        assertThat(tradeRow.getReasoning()).isEqualTo("Filled 20 @ 0.5 (YES)");
    }

    @Test
    void evaluatesTrue_butTradeIsSkipped_stillWritesTwoAuditLogsWithNoOrderIdOnTheSecond() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(true));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        stubOpenMarket(strategy, MARKET_ID);
        when(strategyEvaluationService.evaluate(any(), eq(BINANCE_SYMBOL), eq(MARKET_ID))).thenReturn(true);
        when(openTradeService.openTrade(strategy, MARKET_ID)).thenReturn(OpenTradeResult.skipped("Market is not OPEN"));

        strategyService.runEvaluation(strategy.getId());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(captor.capture());
        AuditLog tradeRow = captor.getAllValues().get(1);
        assertThat(tradeRow.getAction()).isEqualTo(ActionType.ORDER_SKIPPED);
        assertThat(tradeRow.getOrderId()).isNull();
        assertThat(tradeRow.getReasoning()).isEqualTo("Market is not OPEN");
    }

    @Test
    void writesFailedAuditLogWhenEvaluationThrows() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(true));
        when(strategyRepository.findById(strategy.getId())).thenReturn(Optional.of(strategy));
        stubOpenMarket(strategy, MARKET_ID);
        when(strategyEvaluationService.evaluate(any(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("not enough closed candles"));

        strategyService.runEvaluation(strategy.getId());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getReasoning()).contains("not enough closed candles");
        verifyNoInteractions(openTradeService);
    }

    @Test
    void setEnabled_flipsTheFlagAndReschedules() {
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.enabled(false));
        UUID userId = strategy.getUserId();
        when(strategyRepository.findByIdAndUserIdAndDeletedAtIsNull(strategy.getId(), userId))
                .thenReturn(Optional.of(strategy));
        when(strategyRepository.saveAndFlush(strategy)).thenReturn(strategy);
        StrategyView view = StrategyView.builder().id(strategy.getId()).enabled(true).build();
        when(strategyViewMapper.toView(strategy)).thenReturn(view);

        StrategyView result = strategyService.setEnabled(strategy.getId(), userId, true);

        assertThat(strategy.getEnabled()).isTrue();
        assertThat(result).isEqualTo(view);
        verify(strategyScheduler).reschedule(strategy);
    }

    @Test
    void setEnabled_throws404WhenStrategyNotFoundOrNotOwnedByCaller() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(strategyRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategyService.setEnabled(id, userId, true))
                .isInstanceOf(ResponseStatusException.class);
        verify(strategyScheduler, never()).reschedule(any());
    }

    @Test
    void listStrategies_setsTradeCountFromTheBatchedOrderCount() {
        UUID userId = UUID.randomUUID();
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.userId(userId));
        com.polypilot.strategy.dto.view.StrategyView view = com.polypilot.strategy.dto.view.StrategyView.builder()
                .id(strategy.getId())
                .build();

        when(strategyRepository.findAllByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(strategy));
        when(strategyViewMapper.toViews(List.of(strategy))).thenReturn(new java.util.ArrayList<>(List.of(view)));
        when(orderRepository.countByStrategyIdIn(List.of(strategy.getId())))
                .thenReturn(List.of(strategyOrderCount(strategy.getId(), 7)));

        List<com.polypilot.strategy.dto.view.StrategyView> result = strategyService.listStrategies(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTradeCount()).isEqualTo(7);
    }

    @Test
    void listStrategies_defaultsTradeCountToZeroWhenTheStrategyHasNoOrders() {
        UUID userId = UUID.randomUUID();
        Strategy strategy = strategy(UUID.randomUUID(), builder -> builder.userId(userId));
        com.polypilot.strategy.dto.view.StrategyView view = com.polypilot.strategy.dto.view.StrategyView.builder()
                .id(strategy.getId())
                .build();

        when(strategyRepository.findAllByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(strategy));
        when(strategyViewMapper.toViews(List.of(strategy))).thenReturn(new java.util.ArrayList<>(List.of(view)));
        when(orderRepository.countByStrategyIdIn(List.of(strategy.getId()))).thenReturn(List.of());

        List<com.polypilot.strategy.dto.view.StrategyView> result = strategyService.listStrategies(userId);

        assertThat(result.get(0).getTradeCount()).isZero();
    }

    @Test
    void listStrategies_skipsTheBatchQueryWhenThereAreNoStrategies() {
        UUID userId = UUID.randomUUID();
        when(strategyRepository.findAllByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of());
        when(strategyViewMapper.toViews(List.of())).thenReturn(new java.util.ArrayList<>());

        List<com.polypilot.strategy.dto.view.StrategyView> result = strategyService.listStrategies(userId);

        assertThat(result).isEmpty();
        verifyNoInteractions(orderRepository);
    }

    private com.polypilot.repository.StrategyOrderCountView strategyOrderCount(UUID strategyId, long count) {
        return new com.polypilot.repository.StrategyOrderCountView() {
            @Override
            public UUID getStrategyId() {
                return strategyId;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private void stubOpenMarket(Strategy strategy, String marketId) {
        when(marketRepository.findFirstBySeriesIdAndStatusOrderByCreatedAtDesc(
                strategy.getSeries().getId(), MarketStatus.OPEN))
                .thenReturn(Optional.of(Market.builder().id(marketId).build()));
    }

    private Strategy strategy(UUID id, java.util.function.Consumer<Strategy.StrategyBuilder> customizer) {
        Ticker asset = Ticker.builder().id(UUID.randomUUID()).binanceSymbol(BINANCE_SYMBOL).build();
        Series series = Series.builder().id(UUID.randomUUID()).asset(asset).build();

        Strategy.StrategyBuilder builder = Strategy.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .name("btc-dip-buy")
                .dryRun(true)
                .cronExpression("0 */5 * * * *")
                .series(series)
                .tokenSide(MarketOutcome.YES)
                .maxBetSize(BigDecimal.TEN)
                .maxDailyExposure(BigDecimal.valueOf(100))
                .ruleTree("{\"type\":\"MARKET_FIELD\",\"field\":\"up_price\",\"operator\":\"GT\",\"value\":0}");
        customizer.accept(builder);
        return builder.build();
    }
}
