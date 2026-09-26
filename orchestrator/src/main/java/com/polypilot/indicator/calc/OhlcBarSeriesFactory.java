package com.polypilot.indicator.calc;

import com.polypilot.ohlc.entity.OhlcCandle;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Converts a chronological (ascending {@code open_time}) run of closed
 * {@link OhlcCandle}s into a TA4J {@link BarSeries}. The caller is responsible
 * for ordering — {@link com.polypilot.ohlc.repository.OhlcCandleRepository
 * #findRecentClosedCandles} returns newest-first and must be reversed first.
 *
 * <p>Built with {@link DecimalNumFactory} rather than the double-backed
 * default, so indicator math stays on {@link java.math.BigDecimal} precision
 * throughout — consistent with how prices/volume are stored everywhere else
 * in this codebase.
 */
@Component
public class OhlcBarSeriesFactory {

    public BarSeries build(String symbol, String timeframe, List<OhlcCandle> candlesAscending) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withName(symbol + ":" + timeframe)
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();

        for (OhlcCandle candle : candlesAscending) {
            OffsetDateTime openTime = candle.getId().getOpenTime();
            OffsetDateTime closeTime = candle.getCloseTime();
            series.barBuilder()
                    .timePeriod(Duration.between(openTime, closeTime))
                    .beginTime(openTime.toInstant())
                    .endTime(closeTime.toInstant())
                    .openPrice(candle.getOpen())
                    .highPrice(candle.getHigh())
                    .lowPrice(candle.getLow())
                    .closePrice(candle.getClose())
                    .volume(candle.getVolume())
                    .add();
        }
        return series;
    }
}
