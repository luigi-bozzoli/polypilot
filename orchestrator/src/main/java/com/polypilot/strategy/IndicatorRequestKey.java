package com.polypilot.strategy;

import lombok.Value;

import java.util.Map;

/**
 * A single {@code (indicatorKey, params)} request — the same shape
 * {@link com.polypilot.indicator.service.IndicatorCalculationService#calculate}
 * takes, minus the {@code binanceSymbol}.
 *
 * <p>{@code @Value} here is deliberate even though the tree it's used from is
 * mutable: this is a small immutable value object used as a {@code Map}/
 * {@code Set} key, so it needs value equality. Two {@link CompareNode}s with
 * the same {@code indicatorKey} and {@code params} produce an equal key,
 * which is what collapses them to a single {@code calculate()} call even when
 * they read different {@code outputField}s from its result.
 */
@Value
public class IndicatorRequestKey {
    String indicatorKey;
    Map<String, String> params;
}
