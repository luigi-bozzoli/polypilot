package com.polypilot.strategy;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MarketFieldCompareNodeTest {

    /** Fixed snapshot used throughout: upPrice=0.6, downPrice=0.4, volume24h=100000, liquidity=10000, confidence=0.9. */
    @Test
    void upPrice_comparesAgainstThreshold() {
        assertThat(numeric(MarketField.UP_PRICE, CompareOperator.GT, "0.55").evaluate(snapshotContext())).isTrue();
        assertThat(numeric(MarketField.UP_PRICE, CompareOperator.LT, "0.55").evaluate(snapshotContext())).isFalse();
    }

    @Test
    void downPrice_comparesAgainstThreshold() {
        assertThat(numeric(MarketField.DOWN_PRICE, CompareOperator.LTE, "0.4").evaluate(snapshotContext())).isTrue();
    }

    @Test
    void volume24h_comparesAgainstThreshold() {
        assertThat(numeric(MarketField.VOLUME_24H, CompareOperator.GT, "50000").evaluate(snapshotContext())).isTrue();
    }

    @Test
    void liquidity_comparesAgainstThreshold() {
        assertThat(numeric(MarketField.LIQUIDITY, CompareOperator.LT, "50000").evaluate(snapshotContext())).isTrue();
    }

    @Test
    void sentimentConfidence_comparesAgainstThreshold() {
        assertThat(numeric(MarketField.SENTIMENT_CONFIDENCE, CompareOperator.GTE, "0.9").evaluate(snapshotContext())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CompareOperator.class, names = {"EQ", "NEQ", "GT", "GTE", "LT", "LTE"})
    void everyCompareOperator_isHandledForNumericFields(CompareOperator operator) {
        // Smoke test: no operator falls through to an unhandled switch branch.
        numeric(MarketField.UP_PRICE, operator, "0.5").evaluate(snapshotContext());
    }

    @Test
    void sentiment_eq_isTrueOnlyWhenMatching() {
        MarketFieldCompareNode node = sentiment(CompareOperator.EQ, SentimentType.BULLISH);
        assertThat(node.evaluate(contextWithSentiment(SentimentType.BULLISH))).isTrue();
        assertThat(node.evaluate(contextWithSentiment(SentimentType.BEARISH))).isFalse();
    }

    @Test
    void sentiment_neq_isTrueOnlyWhenDiffering() {
        MarketFieldCompareNode node = sentiment(CompareOperator.NEQ, SentimentType.BULLISH);
        assertThat(node.evaluate(contextWithSentiment(SentimentType.BEARISH))).isTrue();
        assertThat(node.evaluate(contextWithSentiment(SentimentType.BULLISH))).isFalse();
    }

    @Test
    void missingNumericValue_evaluatesToFalse_notAnException() {
        MarketFieldCompareNode node = numeric(MarketField.UP_PRICE, CompareOperator.GT, "0.55");
        EvaluationContext ctx = new EvaluationContext(java.util.Map.of(),
                new MarketFieldSnapshot(null, null, null, null, null, null));

        assertThat(node.evaluate(ctx)).isFalse();
    }

    @Test
    void missingSentiment_evaluatesToFalse_notAnException() {
        MarketFieldCompareNode node = sentiment(CompareOperator.EQ, SentimentType.BULLISH);
        EvaluationContext ctx = new EvaluationContext(java.util.Map.of(),
                new MarketFieldSnapshot(null, null, null, null, null, null));

        assertThat(node.evaluate(ctx)).isFalse();
    }

    @Test
    void usesMarketFields_isTrue() {
        assertThat(numeric(MarketField.UP_PRICE, CompareOperator.GT, "0.5").usesMarketFields()).isTrue();
        assertThat(sentiment(CompareOperator.EQ, SentimentType.BULLISH).usesMarketFields()).isTrue();
    }

    @Test
    void collectIndicatorRequests_doesNotMutateTheSuppliedSet() {
        MarketFieldCompareNode node = numeric(MarketField.UP_PRICE, CompareOperator.GT, "0.5");
        Set<IndicatorRequestKey> requests = new HashSet<>();

        node.collectIndicatorRequests(requests);

        assertThat(requests).isEmpty();
    }

    private static MarketFieldCompareNode numeric(MarketField field, CompareOperator operator, String threshold) {
        return new MarketFieldCompareNode(field, operator, new BigDecimal(threshold), null);
    }

    private static MarketFieldCompareNode sentiment(CompareOperator operator, SentimentType value) {
        return new MarketFieldCompareNode(MarketField.SENTIMENT, operator, null, value);
    }

    private static EvaluationContext snapshotContext() {
        MarketFieldSnapshot snapshot = new MarketFieldSnapshot(
                new BigDecimal("0.6"), new BigDecimal("0.4"), new BigDecimal("100000"),
                new BigDecimal("10000"), SentimentType.NEUTRAL, new BigDecimal("0.9"));
        return new EvaluationContext(java.util.Map.of(), snapshot);
    }

    private static EvaluationContext contextWithSentiment(SentimentType sentiment) {
        MarketFieldSnapshot snapshot = new MarketFieldSnapshot(null, null, null, null, sentiment, null);
        return new EvaluationContext(java.util.Map.of(), snapshot);
    }
}
