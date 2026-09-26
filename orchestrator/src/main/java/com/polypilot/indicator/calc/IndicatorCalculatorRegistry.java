package com.polypilot.indicator.calc;

import com.polypilot.indicator.calc.source.PriceSourceResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.MMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.TRIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps a catalog {@code indicators.key} to the {@link IndicatorCalculator}
 * that computes it. This is the single place new indicators get wired up —
 * see the class-level note in {@code IndicatorCalculationService} for the
 * end-to-end flow. Adding indicator #7 means one more {@link #register} call
 * here (plus its {@code 002_seed_data.sql} rows); nothing else in this
 * package changes.
 *
 * <p>Every calculator reads {@link #valueAt} for its result(s) — the TA4J
 * {@link Indicator#getValue(int)} at the series' last index, converted to
 * {@link BigDecimal}, matching the "latest value only" calling contract.
 */
@Component
public class IndicatorCalculatorRegistry {

    private final Map<String, IndicatorCalculator> calculators = new HashMap<>();

    public IndicatorCalculatorRegistry() {
        register("sma", (series, params) -> {
            Indicator<Num> source = PriceSourceResolver.resolve(series, params.getString("source"));
            SMAIndicator sma = new SMAIndicator(source, params.getInt("period"));
            return Map.of("value", valueAt(sma, series));
        });

        register("ema", (series, params) -> {
            Indicator<Num> source = PriceSourceResolver.resolve(series, params.getString("source"));
            EMAIndicator ema = new EMAIndicator(source, params.getInt("period"));
            return Map.of("value", valueAt(ema, series));
        });

        register("rsi", (series, params) -> {
            Indicator<Num> source = PriceSourceResolver.resolve(series, params.getString("source"));
            RSIIndicator rsi = new RSIIndicator(source, params.getInt("period"));
            return Map.of("value", valueAt(rsi, series));
        });

        register("macd", (series, params) -> {
            Indicator<Num> source = PriceSourceResolver.resolve(series, params.getString("source"));
            int signalPeriod = params.getInt("signal_period");
            MACDIndicator macd = new MACDIndicator(source, params.getInt("fast_period"), params.getInt("slow_period"));
            return Map.of(
                    "macd", valueAt(macd, series),
                    "signal", valueAt(macd.getSignalLine(signalPeriod), series),
                    "histogram", valueAt(macd.getHistogram(signalPeriod), series));
        });

        // ATR has no `source` parameter — true range is defined on high/low/prior
        // close, not a single price series. `smoothing_method` picks which moving
        // average smooths it; "wilder" (TA-Lib's own default) is TA4J's MMAIndicator.
        register("atr", (series, params) -> {
            TRIndicator trueRange = new TRIndicator(series);
            int period = params.getInt("period");
            Indicator<Num> atr = switch (params.getString("smoothing_method")) {
                case "sma" -> new SMAIndicator(trueRange, period);
                case "ema" -> new EMAIndicator(trueRange, period);
                case "wilder" -> new MMAIndicator(trueRange, period);
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown smoothing_method: " + params.getString("smoothing_method"));
            };
            return Map.of("value", valueAt(atr, series));
        });

        // Volume MA has no `source` either — it averages the volume field, not price.
        register("volume_ma", (series, params) -> {
            VolumeIndicator volume = new VolumeIndicator(series);
            int period = params.getInt("period");
            Indicator<Num> ma = switch (params.getString("ma_type")) {
                case "sma" -> new SMAIndicator(volume, period);
                case "ema" -> new EMAIndicator(volume, period);
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown ma_type: " + params.getString("ma_type"));
            };
            return Map.of("value", valueAt(ma, series));
        });
    }

    public IndicatorCalculator get(String indicatorKey) {
        IndicatorCalculator calculator = calculators.get(indicatorKey);
        if (calculator == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED, "No calculation implemented yet for indicator: " + indicatorKey);
        }
        return calculator;
    }

    private void register(String indicatorKey, IndicatorCalculator calculator) {
        if (calculators.putIfAbsent(indicatorKey, calculator) != null) {
            throw new IllegalStateException("Duplicate indicator calculator registration: " + indicatorKey);
        }
    }

    private static BigDecimal valueAt(Indicator<Num> indicator, BarSeries series) {
        return indicator.getValue(series.getEndIndex()).bigDecimalValue();
    }
}
