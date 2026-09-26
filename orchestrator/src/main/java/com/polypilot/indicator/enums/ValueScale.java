package com.polypilot.indicator.enums;

/**
 * The numeric range an indicator output lives in. Consumed by the later
 * comparison UI to decide what an output can sensibly be compared against
 * (a constant, a band, or another output). Mirrors the {@code value_scale}
 * CHECK constraint in {@code 001_schema.sql}.
 */
public enum ValueScale {
    /** Same units as price (moving averages, ATR). */
    PRICE,
    /** Bounded 0-100 oscillator (RSI). */
    OSCILLATOR_0_100,
    /** No fixed bounds; can be negative (MACD line / signal / histogram). */
    UNBOUNDED,
    /** Traded-volume units (Volume MA). */
    VOLUME
}
