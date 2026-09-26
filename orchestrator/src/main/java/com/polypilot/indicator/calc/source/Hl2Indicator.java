package com.polypilot.indicator.calc.source;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.Num;

/**
 * {@code (high + low) / 2} at each bar — one of the {@code source} values a
 * catalog indicator parameter can select ({@code universal_parameters} row
 * {@code source}). TA4J ships open/high/low/close price indicators natively
 * but not this or its siblings ({@link Hlc3Indicator}, {@link Ohlc4Indicator}),
 * so they're composed here from the primitives it does provide.
 */
public class Hl2Indicator extends CachedIndicator<Num> {

    private final HighPriceIndicator high;
    private final LowPriceIndicator low;

    public Hl2Indicator(BarSeries series) {
        super(series);
        this.high = new HighPriceIndicator(series);
        this.low = new LowPriceIndicator(series);
    }

    @Override
    protected Num calculate(int index) {
        Num two = getBarSeries().numFactory().two();
        return high.getValue(index).plus(low.getValue(index)).dividedBy(two);
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
