package com.polypilot.indicator.calc;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Small, deterministic {@link BarSeries} builders shared by the indicator
 * calculation tests — mirrors what {@link OhlcBarSeriesFactory} produces from
 * real candles, but without a DB round-trip.
 */
public final class BarSeriesFixtures {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private BarSeriesFixtures() {
    }

    /** A flat series where open = high = low = close for every bar. */
    public static BarSeries flatCloseSeries(double... closes) {
        return ohlcvSeries(closes, closes, closes, closes, null);
    }

    public static BarSeries ohlcvSeries(double[] open, double[] high, double[] low, double[] close, double[] volume) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withName("test-series")
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();

        Instant begin = Instant.EPOCH;
        for (int i = 0; i < close.length; i++) {
            series.barBuilder()
                    .timePeriod(ONE_MINUTE)
                    .beginTime(begin)
                    .endTime(begin.plus(ONE_MINUTE))
                    .openPrice(open[i])
                    .highPrice(high[i])
                    .lowPrice(low[i])
                    .closePrice(close[i])
                    .volume(volume != null ? volume[i] : 1.0)
                    .add();
            begin = begin.plus(ONE_MINUTE);
        }
        return series;
    }
}
