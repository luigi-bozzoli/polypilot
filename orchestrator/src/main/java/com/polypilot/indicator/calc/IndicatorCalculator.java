package com.polypilot.indicator.calc;

import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Computes one catalog indicator's output(s) at the most recent bar of a
 * {@link BarSeries}. One instance per {@code indicators.key}, registered in
 * {@link IndicatorCalculatorRegistry}.
 *
 * <p>Single-output indicators (SMA, EMA, RSI, ATR, Volume MA) return a
 * one-entry map keyed {@code "value"}; multi-output indicators (MACD) return
 * one entry per {@code indicator_outputs} row — see the seeded catalog in
 * {@code 002_seed_data.sql} for the exact key set per indicator.
 */
@FunctionalInterface
public interface IndicatorCalculator {

    Map<String, BigDecimal> calculate(BarSeries series, IndicatorParams params);
}
