package com.polypilot.indicator.calc.source;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.Num;

/**
 * {@code (high + low + close) / 3} at each bar — the "typical price". See
 * {@link Hl2Indicator} for why this is hand-composed rather than a TA4J
 * built-in.
 */
public class Hlc3Indicator extends CachedIndicator<Num> {

    private final HighPriceIndicator high;
    private final LowPriceIndicator low;
    private final ClosePriceIndicator close;

    public Hlc3Indicator(BarSeries series) {
        super(series);
        this.high = new HighPriceIndicator(series);
        this.low = new LowPriceIndicator(series);
        this.close = new ClosePriceIndicator(series);
    }

    @Override
    protected Num calculate(int index) {
        Num three = getBarSeries().numFactory().three();
        return high.getValue(index)
                .plus(low.getValue(index))
                .plus(close.getValue(index))
                .dividedBy(three);
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
