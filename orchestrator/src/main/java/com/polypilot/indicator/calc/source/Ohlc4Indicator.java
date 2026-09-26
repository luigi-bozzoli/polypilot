package com.polypilot.indicator.calc.source;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.Num;

/**
 * {@code (open + high + low + close) / 4} at each bar. See {@link Hl2Indicator}
 * for why this is hand-composed rather than a TA4J built-in.
 */
public class Ohlc4Indicator extends CachedIndicator<Num> {

    private final OpenPriceIndicator open;
    private final HighPriceIndicator high;
    private final LowPriceIndicator low;
    private final ClosePriceIndicator close;

    public Ohlc4Indicator(BarSeries series) {
        super(series);
        this.open = new OpenPriceIndicator(series);
        this.high = new HighPriceIndicator(series);
        this.low = new LowPriceIndicator(series);
        this.close = new ClosePriceIndicator(series);
    }

    @Override
    protected Num calculate(int index) {
        Num four = getBarSeries().numFactory().numOf(4);
        return open.getValue(index)
                .plus(high.getValue(index))
                .plus(low.getValue(index))
                .plus(close.getValue(index))
                .dividedBy(four);
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
