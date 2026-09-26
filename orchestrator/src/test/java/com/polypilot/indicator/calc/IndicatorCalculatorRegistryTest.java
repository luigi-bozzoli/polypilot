package com.polypilot.indicator.calc;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises every seeded indicator through the real {@link IndicatorCalculatorRegistry}
 * wiring. Fixtures are deliberately degenerate (flat or monotonic series) so the
 * expected values are exact/obvious without needing an external reference
 * implementation — the point is to catch wiring mistakes (wrong TA4J class,
 * wrong constructor argument order, wrong output key), not to re-verify TA4J's
 * own math.
 */
class IndicatorCalculatorRegistryTest {

    private final IndicatorCalculatorRegistry registry = new IndicatorCalculatorRegistry();

    @Test
    void sma_onFlatSeries_equalsTheFlatValue() {
        BarSeries series = BarSeriesFixtures.flatCloseSeries(doubles(30, 100.0));
        IndicatorParams params = paramsOf(Map.of("period", "5", "source", "close"));

        Map<String, BigDecimal> result = registry.get("sma").calculate(series, params);

        assertThat(result).hasSize(1);
        assertThat(result.get("value")).isEqualByComparingTo("100");
    }

    @Test
    void ema_onFlatSeries_equalsTheFlatValue() {
        BarSeries series = BarSeriesFixtures.flatCloseSeries(doubles(30, 42.5));
        IndicatorParams params = paramsOf(Map.of("period", "10", "source", "close"));

        Map<String, BigDecimal> result = registry.get("ema").calculate(series, params);

        assertThat(result.get("value")).isEqualByComparingTo("42.5");
    }

    @Test
    void rsi_onStrictlyIncreasingSeries_isMaxedOut() {
        double[] closes = new double[30];
        for (int i = 0; i < closes.length; i++) {
            closes[i] = i + 1; // every bar is a gain, never a loss
        }
        BarSeries series = BarSeriesFixtures.flatCloseSeries(closes);
        IndicatorParams params = paramsOf(Map.of("period", "14", "source", "close"));

        Map<String, BigDecimal> result = registry.get("rsi").calculate(series, params);

        assertThat(result.get("value")).isEqualByComparingTo("100");
    }

    @Test
    void macd_onFlatSeries_isZeroAcrossAllThreeOutputs() {
        BarSeries series = BarSeriesFixtures.flatCloseSeries(doubles(60, 10.0));
        IndicatorParams params = paramsOf(Map.of(
                "fast_period", "12", "slow_period", "26", "signal_period", "9", "source", "close"));

        Map<String, BigDecimal> result = registry.get("macd").calculate(series, params);

        assertThat(result).containsOnlyKeys("macd", "signal", "histogram");
        assertThat(result.get("macd")).isEqualByComparingTo("0");
        assertThat(result.get("signal")).isEqualByComparingTo("0");
        assertThat(result.get("histogram")).isEqualByComparingTo("0");
    }

    @Test
    void atr_onZeroRangeSeries_isZeroForEverySmoothingMethod() {
        double[] flat = doubles(30, 50.0);
        BarSeries series = BarSeriesFixtures.ohlcvSeries(flat, flat, flat, flat, null);

        for (String smoothing : new String[] {"wilder", "sma", "ema"}) {
            IndicatorParams params = paramsOf(Map.of("period", "14", "smoothing_method", smoothing));
            Map<String, BigDecimal> result = registry.get("atr").calculate(series, params);
            assertThat(result.get("value")).as("smoothing=%s", smoothing).isEqualByComparingTo("0");
        }
    }

    @Test
    void atr_unknownSmoothingMethod_throwsBadRequest() {
        BarSeries series = BarSeriesFixtures.flatCloseSeries(doubles(20, 50.0));
        IndicatorParams params = paramsOf(Map.of("period", "14", "smoothing_method", "bogus"));

        assertThatThrownBy(() -> registry.get("atr").calculate(series, params))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void volumeMa_onFlatVolumeSeries_equalsTheFlatVolume_forBothMaTypes() {
        double[] closes = doubles(30, 10.0);
        double[] volumes = doubles(30, 500.0);
        BarSeries series = BarSeriesFixtures.ohlcvSeries(closes, closes, closes, closes, volumes);

        for (String maType : new String[] {"sma", "ema"}) {
            IndicatorParams params = paramsOf(Map.of("period", "20", "ma_type", maType));
            Map<String, BigDecimal> result = registry.get("volume_ma").calculate(series, params);
            assertThat(result.get("value")).as("ma_type=%s", maType).isEqualByComparingTo("500");
        }
    }

    @Test
    void get_unknownKey_throwsNotImplemented() {
        assertThatThrownBy(() -> registry.get("bollinger_bands"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    private static IndicatorParams paramsOf(Map<String, String> values) {
        return new IndicatorParams(values);
    }

    private static double[] doubles(int count, double value) {
        double[] values = new double[count];
        java.util.Arrays.fill(values, value);
        return values;
    }
}
