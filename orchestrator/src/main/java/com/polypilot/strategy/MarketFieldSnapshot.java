package com.polypilot.strategy;

import com.polypilot.enums.SentimentType;
import lombok.Value;

import java.math.BigDecimal;

/**
 * One market's static fields resolved for a single {@link StrategyEvaluationService#evaluate}
 * call — the {@link MarketFieldCompareNode} counterpart to indicator results in
 * {@link EvaluationContext}. Built once per evaluation by {@link MarketFieldResolver}, regardless
 * of how many {@link MarketFieldCompareNode}s the tree contains.
 *
 * <p>Every field may be {@code null} — a market's price/volume columns can be unset, and there
 * may be no {@code SentimentScore} yet. {@link MarketFieldCompareNode} treats a {@code null}
 * required value as a {@code false} evaluation rather than failing, per the product behavior in
 * the strategy-tree design.
 */
@Value
public class MarketFieldSnapshot {
    BigDecimal upPrice;
    BigDecimal downPrice;
    BigDecimal volume24h;
    BigDecimal liquidity;
    SentimentType sentiment;
    BigDecimal sentimentConfidence;
}
