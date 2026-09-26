package com.polypilot.strategy;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.enums.MarketField;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluationContextTest {

    private static final IndicatorRequestKey RSI = new IndicatorRequestKey("rsi", Map.of("period", "14"));
    private static final IndicatorRequestKey MACD = new IndicatorRequestKey("macd", Map.of("fast_period", "12"));

    @Test
    void valueFor_returnsTheStoredValue() {
        EvaluationContext ctx = new EvaluationContext(Map.of(RSI, Map.of("value", BigDecimal.valueOf(55))), null);

        assertThat(ctx.valueFor(RSI, "value")).isEqualByComparingTo("55");
    }

    @Test
    void valueFor_missingIndicatorKey_throwsIllegalState() {
        EvaluationContext ctx = new EvaluationContext(Map.of(RSI, Map.of("value", BigDecimal.valueOf(55))), null);

        assertThatThrownBy(() -> ctx.valueFor(MACD, "macd"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No pre-computed result for");
    }

    @Test
    void valueFor_missingOutputField_throwsIllegalState() {
        EvaluationContext ctx = new EvaluationContext(Map.of(MACD, Map.of("macd", BigDecimal.valueOf(1.5))), null);

        assertThatThrownBy(() -> ctx.valueFor(MACD, "signal"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no output field [signal]");
    }

    @Test
    void multiOutputIndicator_sharesOneEntryAcrossDifferentOutputFields() {
        EvaluationContext ctx = new EvaluationContext(Map.of(
                MACD, Map.of("macd", BigDecimal.valueOf(1.5), "signal", BigDecimal.valueOf(1.2))), null);

        assertThat(ctx.valueFor(MACD, "macd")).isEqualByComparingTo("1.5");
        assertThat(ctx.valueFor(MACD, "signal")).isEqualByComparingTo("1.2");
    }

    @Test
    void indicatorRequestKey_equalsByIndicatorKeyAndParams_notIdentity() {
        IndicatorRequestKey a = new IndicatorRequestKey("rsi", Map.of("period", "14"));
        IndicatorRequestKey b = new IndicatorRequestKey("rsi", Map.of("period", "14"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void marketFieldValue_mapsEveryNumericFieldToItsSnapshotProperty() {
        MarketFieldSnapshot snapshot = new MarketFieldSnapshot(
                new BigDecimal("0.60"), new BigDecimal("0.40"), new BigDecimal("100000"),
                new BigDecimal("10000"), SentimentType.BULLISH, new BigDecimal("0.9"));
        EvaluationContext ctx = new EvaluationContext(Map.of(), snapshot);

        assertThat(ctx.marketFieldValue(MarketField.UP_PRICE)).isEqualByComparingTo("0.60");
        assertThat(ctx.marketFieldValue(MarketField.DOWN_PRICE)).isEqualByComparingTo("0.40");
        assertThat(ctx.marketFieldValue(MarketField.VOLUME_24H)).isEqualByComparingTo("100000");
        assertThat(ctx.marketFieldValue(MarketField.LIQUIDITY)).isEqualByComparingTo("10000");
        assertThat(ctx.marketFieldValue(MarketField.SENTIMENT_CONFIDENCE)).isEqualByComparingTo("0.9");
    }

    @Test
    void sentimentValue_returnsTheSnapshotSentiment() {
        MarketFieldSnapshot snapshot = new MarketFieldSnapshot(null, null, null, null, SentimentType.BEARISH, null);
        EvaluationContext ctx = new EvaluationContext(Map.of(), snapshot);

        assertThat(ctx.sentimentValue()).isEqualTo(SentimentType.BEARISH);
    }

    @Test
    void marketFieldValue_sentimentIsNotAllowed_throwsIllegalState() {
        MarketFieldSnapshot snapshot = new MarketFieldSnapshot(null, null, null, null, SentimentType.BEARISH, null);
        EvaluationContext ctx = new EvaluationContext(Map.of(), snapshot);

        assertThatThrownBy(() -> ctx.marketFieldValue(MarketField.SENTIMENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SENTIMENT");
    }

    @Test
    void marketFieldValue_missingSnapshot_throwsIllegalState() {
        EvaluationContext ctx = new EvaluationContext(Map.of(), null);

        assertThatThrownBy(() -> ctx.marketFieldValue(MarketField.UP_PRICE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No market field snapshot");
    }

    @Test
    void sentimentValue_missingSnapshot_throwsIllegalState() {
        EvaluationContext ctx = new EvaluationContext(Map.of(), null);

        assertThatThrownBy(ctx::sentimentValue)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No market field snapshot");
    }
}
