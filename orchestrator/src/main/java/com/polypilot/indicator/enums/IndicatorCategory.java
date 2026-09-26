package com.polypilot.indicator.enums;

/**
 * Broad grouping used to organize the indicator catalog in the strategy builder.
 * Persisted as a string; mirrors the {@code indicators.category} CHECK constraint
 * in {@code 001_schema.sql}.
 */
public enum IndicatorCategory {
    TREND,
    MOMENTUM,
    VOLATILITY,
    VOLUME
}
