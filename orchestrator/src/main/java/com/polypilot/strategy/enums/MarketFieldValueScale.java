package com.polypilot.strategy.enums;

/**
 * The numeric range a {@link MarketField} lives in, surfaced in
 * {@code GET /strategies/condition-fields}'s catalog as a display/UI hint — same purpose as
 * {@code com.polypilot.indicator.enums.ValueScale}, kept as a separate small enum since market
 * fields use a different, smaller set of scales (probabilities are 0-1, not 0-100).
 */
public enum MarketFieldValueScale {
    /** 0-1 bounded probability/confidence (YES/NO price, sentiment confidence). */
    OSCILLATOR_0_1,
    /** Traded-volume / liquidity units. */
    VOLUME
}
