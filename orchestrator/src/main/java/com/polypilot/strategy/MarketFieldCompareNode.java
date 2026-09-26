package com.polypilot.strategy;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Leaf node: compares one static market/sentiment field against a threshold. The
 * {@link MarketField} counterpart to {@link CompareNode} — resolved from a
 * {@link MarketFieldSnapshot} (via {@link MarketFieldResolver}) rather than from
 * {@link com.polypilot.indicator.service.IndicatorCalculationService}, since market fields are
 * not indicators.
 *
 * <p>{@link #threshold} backs every numeric field ({@code UP_PRICE}, {@code DOWN_PRICE},
 * {@code VOLUME_24H}, {@code LIQUIDITY}, {@code SENTIMENT_CONFIDENCE}); {@link #sentimentValue}
 * backs {@code SENTIMENT} only, compared via {@code EQ}/{@code NEQ} — sentiment is a label, not
 * an orderable value, so {@code GT}/{@code GTE}/{@code LT}/{@code LTE} are not applied here (the
 * API layer rejects them before a tree is ever built).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketFieldCompareNode extends StrategyNode {

    private MarketField field;
    private CompareOperator operator;

    /** Numeric fields only; {@code null} for {@code SENTIMENT}. */
    private BigDecimal threshold;

    /** {@code SENTIMENT} only; {@code null} for every numeric field. */
    private SentimentType sentimentValue;

    @Override
    public boolean evaluate(EvaluationContext ctx) {
        if (field == MarketField.SENTIMENT) {
            SentimentType actual = ctx.sentimentValue();
            if (actual == null) {
                return false;
            }
            return switch (operator) {
                case EQ -> actual == sentimentValue;
                case NEQ -> actual != sentimentValue;
                default -> throw new IllegalStateException(
                        "Operator [" + operator + "] is not valid for SENTIMENT — only EQ/NEQ are");
            };
        }

        BigDecimal actual = ctx.marketFieldValue(field);
        if (actual == null) {
            return false;
        }
        int comparison = actual.compareTo(threshold);
        return switch (operator) {
            case EQ -> comparison == 0;
            case NEQ -> comparison != 0;
            case GT -> comparison > 0;
            case GTE -> comparison >= 0;
            case LT -> comparison < 0;
            case LTE -> comparison <= 0;
        };
    }

    /** Market fields are not indicators — nothing to collect. */
    @Override
    public void collectIndicatorRequests(Set<IndicatorRequestKey> out) {
        // no-op by design
    }

    @Override
    public boolean usesMarketFields() {
        return true;
    }
}
