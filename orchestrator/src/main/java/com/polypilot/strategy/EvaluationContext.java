package com.polypilot.strategy;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.enums.MarketField;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pre-computed results for one {@link StrategyEvaluationService#evaluate} call, threaded
 * explicitly through every {@link StrategyNode#evaluate} call rather than held as a resolver
 * reference on nodes — see the design doc's "Result delivery" decision.
 *
 * <p>{@link #indicatorResults} backs {@link CompareNode} lookups via {@link #valueFor};
 * {@link #marketFieldSnapshot} backs {@link MarketFieldCompareNode} lookups via
 * {@link #marketFieldValue} / {@link #sentimentValue}. {@code marketFieldSnapshot} is {@code null}
 * whenever the tree being evaluated has no market-field leaves (see
 * {@link StrategyNode#usesMarketFields}) — {@code indicatorResults} is always populated, even if
 * empty, since indicator collection always runs.
 *
 * <p>A miss in {@link #valueFor} is always a bug in the collection pass (a node that wasn't
 * walked when {@link StrategyNode#collectIndicatorRequests} ran) or a catalog/tree drift, never a
 * user input error — hence the unchecked exception, matching the convention already used in
 * {@link com.polypilot.indicator.calc.IndicatorParams}. A {@code null} field/snapshot in
 * {@link #marketFieldValue} / {@link #sentimentValue} is a legitimate outcome (missing market
 * data), never thrown as an error — see {@link MarketFieldCompareNode#evaluate}, which is where
 * the "missing data ⇒ false" product behavior actually lives.
 */
@Value
public class EvaluationContext {
    Map<IndicatorRequestKey, Map<String, BigDecimal>> indicatorResults;
    MarketFieldSnapshot marketFieldSnapshot;

    public BigDecimal valueFor(IndicatorRequestKey key, String outputField) {
        Map<String, BigDecimal> outputs = indicatorResults.get(key);
        if (outputs == null) {
            throw new IllegalStateException(
                    "No pre-computed result for " + key + " — indicator collection pass missed a node");
        }
        BigDecimal value = outputs.get(outputField);
        if (value == null) {
            throw new IllegalStateException(
                    "Indicator " + key.getIndicatorKey() + " has no output field [" + outputField + "]");
        }
        return value;
    }

    /**
     * The numeric value of one market field. {@code null} means the underlying data is missing
     * (e.g. a market whose {@code volume_24h} hasn't been synced yet) — callers must treat that
     * as "condition not met", not coerce it to zero. Never call this with {@link MarketField#SENTIMENT}
     * — sentiment is a label, not a number; use {@link #sentimentValue()} instead.
     */
    public BigDecimal marketFieldValue(MarketField field) {
        if (marketFieldSnapshot == null) {
            throw new IllegalStateException(
                    "No market field snapshot in this context — market-field collection pass missed a node");
        }
        return switch (field) {
            case UP_PRICE -> marketFieldSnapshot.getUpPrice();
            case DOWN_PRICE -> marketFieldSnapshot.getDownPrice();
            case VOLUME_24H -> marketFieldSnapshot.getVolume24h();
            case LIQUIDITY -> marketFieldSnapshot.getLiquidity();
            case SENTIMENT_CONFIDENCE -> marketFieldSnapshot.getSentimentConfidence();
            case SENTIMENT -> throw new IllegalStateException(
                    "SENTIMENT is not a numeric market field — use sentimentValue() instead");
        };
    }

    /** The resolved AI sentiment label, or {@code null} if there is no sentiment score yet. */
    public SentimentType sentimentValue() {
        if (marketFieldSnapshot == null) {
            throw new IllegalStateException(
                    "No market field snapshot in this context — market-field collection pass missed a node");
        }
        return marketFieldSnapshot.getSentiment();
    }
}
