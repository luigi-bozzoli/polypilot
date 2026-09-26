package com.polypilot.strategy;

import com.polypilot.enums.SentimentType;
import com.polypilot.indicator.service.IndicatorCalculationService;
import com.polypilot.strategy.enums.BooleanOperator;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.UnaryOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Exercises the two-phase evaluation flow with the real tree/node classes and a real (small)
 * executor, mocking only {@link IndicatorCalculationService} and {@link MarketFieldResolver} —
 * the DB/candle and DB/market machinery they front are covered by their own tests.
 */
@ExtendWith(MockitoExtension.class)
class StrategyEvaluationServiceTest {

    private static final String SYMBOL = "BTCUSDT";
    private static final String MARKET_ID = "0xabc";
    private static final IndicatorRequestKey RSI = new IndicatorRequestKey("rsi", Map.of("period", "14"));
    private static final IndicatorRequestKey MACD = new IndicatorRequestKey("macd", Map.of("fast_period", "12"));

    @Mock
    private IndicatorCalculationService indicatorCalculationService;

    @Mock
    private MarketFieldResolver marketFieldResolver;

    private ThreadPoolTaskExecutor executor;
    private StrategyEvaluationService service;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.initialize();
        service = new StrategyEvaluationService(indicatorCalculationService, marketFieldResolver, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void singleCompareNode_evaluatesUsingTheComputedIndicator() {
        when(indicatorCalculationService.calculate(SYMBOL, "rsi", RSI.getParams()))
                .thenReturn(Map.of("value", BigDecimal.valueOf(80)));
        CompareNode tree = new CompareNode(new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verifyNoInteractions(marketFieldResolver);
    }

    @Test
    void sameIndicatorReferencedTwice_isCalculatedOnlyOnce() {
        when(indicatorCalculationService.calculate(SYMBOL, "macd", MACD.getParams()))
                .thenReturn(Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.2)));

        CompareNode macdAboveSignal = new CompareNode(new IndicatorOperand(MACD, "macd"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(1.0)));
        CompareNode signalPositive = new CompareNode(new IndicatorOperand(MACD, "signal"), CompareOperator.GT, new LiteralOperand(BigDecimal.ZERO));
        BooleanNode tree = new BooleanNode(BooleanOperator.AND, macdAboveSignal, signalPositive);

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verify(indicatorCalculationService, times(1)).calculate(anyString(), anyString(), anyMap());
    }

    @Test
    void mixedTree_withNotAndBooleanNodes_evaluatesEndToEnd() {
        when(indicatorCalculationService.calculate(SYMBOL, "rsi", RSI.getParams()))
                .thenReturn(Map.of("value", BigDecimal.valueOf(30)));
        when(indicatorCalculationService.calculate(SYMBOL, "macd", MACD.getParams()))
                .thenReturn(Map.of("macd", BigDecimal.valueOf(-0.5)));

        // NOT(rsi.value > 70) AND (macd.macd < 0)
        UnaryBooleanNode notOverbought = new UnaryBooleanNode(UnaryOperator.NOT,
                new CompareNode(new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70))));
        CompareNode macdNegative = new CompareNode(new IndicatorOperand(MACD, "macd"), CompareOperator.LT, new LiteralOperand(BigDecimal.ZERO));
        BooleanNode tree = new BooleanNode(BooleanOperator.AND, notOverbought, macdNegative);

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
    }

    @Test
    void indicatorVsIndicatorTree_resolvesBothSidesFromTheirOwnCalculation() {
        when(indicatorCalculationService.calculate(SYMBOL, "rsi", RSI.getParams()))
                .thenReturn(Map.of("value", BigDecimal.valueOf(80)));
        when(indicatorCalculationService.calculate(SYMBOL, "macd", MACD.getParams()))
                .thenReturn(Map.of("signal", BigDecimal.valueOf(1.2)));

        CompareNode tree = new CompareNode(
                new IndicatorOperand(RSI, "value"), CompareOperator.GT, new IndicatorOperand(MACD, "signal"));

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verify(indicatorCalculationService, times(1)).calculate(SYMBOL, "rsi", RSI.getParams());
        verify(indicatorCalculationService, times(1)).calculate(SYMBOL, "macd", MACD.getParams());
    }

    @Test
    void failedIndicatorComputation_abortsTheWholeEvaluation() {
        when(indicatorCalculationService.calculate(SYMBOL, "rsi", RSI.getParams()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT,
                        "Not enough closed candles"));
        CompareNode tree = new CompareNode(new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));

        assertThatThrownBy(() -> service.evaluate(tree, SYMBOL, MARKET_ID))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ResponseStatusException.class);
    }

    @Test
    void marketFieldOnlyTree_resolvesTheSnapshotAndSkipsIndicatorCalculation() {
        when(marketFieldResolver.resolve(MARKET_ID)).thenReturn(new MarketFieldSnapshot(
                new BigDecimal("0.6"), new BigDecimal("0.4"), null, null, null, null));
        MarketFieldCompareNode tree = new MarketFieldCompareNode(
                MarketField.UP_PRICE, CompareOperator.GT, new BigDecimal("0.5"), null);

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verifyNoInteractions(indicatorCalculationService);
    }

    @Test
    void indicatorOnlyTree_doesNotInvokeMarketFieldResolver() {
        when(indicatorCalculationService.calculate(SYMBOL, "rsi", RSI.getParams()))
                .thenReturn(Map.of("value", BigDecimal.valueOf(80)));
        CompareNode tree = new CompareNode(new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verify(marketFieldResolver, never()).resolve(any());
    }

    @Test
    void mixedIndicatorAndMarketFieldTree_resolvesBoth() {
        when(indicatorCalculationService.calculate(SYMBOL, "rsi", RSI.getParams()))
                .thenReturn(Map.of("value", BigDecimal.valueOf(80)));
        when(marketFieldResolver.resolve(MARKET_ID)).thenReturn(new MarketFieldSnapshot(
                new BigDecimal("0.6"), null, null, null, SentimentType.BULLISH, null));

        CompareNode indicatorLeaf = new CompareNode(new IndicatorOperand(RSI, "value"), CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));
        MarketFieldCompareNode marketFieldLeaf = new MarketFieldCompareNode(
                MarketField.SENTIMENT, CompareOperator.EQ, null, SentimentType.BULLISH);
        BooleanNode tree = new BooleanNode(BooleanOperator.AND, indicatorLeaf, marketFieldLeaf);

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verify(indicatorCalculationService, times(1)).calculate(anyString(), anyString(), anyMap());
        verify(marketFieldResolver, times(1)).resolve(MARKET_ID);
    }

    @Test
    void multipleMarketFieldLeaves_resolveTheSnapshotExactlyOnce() {
        when(marketFieldResolver.resolve(MARKET_ID)).thenReturn(new MarketFieldSnapshot(
                new BigDecimal("0.6"), new BigDecimal("0.4"), new BigDecimal("100000"), null, null, null));

        MarketFieldCompareNode upPrice = new MarketFieldCompareNode(
                MarketField.UP_PRICE, CompareOperator.GT, new BigDecimal("0.5"), null);
        MarketFieldCompareNode volume = new MarketFieldCompareNode(
                MarketField.VOLUME_24H, CompareOperator.GT, new BigDecimal("50000"), null);
        BooleanNode tree = new BooleanNode(BooleanOperator.AND, upPrice, volume);

        assertThat(service.evaluate(tree, SYMBOL, MARKET_ID)).isTrue();
        verify(marketFieldResolver, times(1)).resolve(MARKET_ID);
    }
}
