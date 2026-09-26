package com.polypilot.indicator.calc.source;

import com.polypilot.indicator.calc.BarSeriesFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@code source} parameter's derived-price options (hl2/hlc3/ohlc4), plus
 * {@link PriceSourceResolver} dispatch to all seven allowed values (see
 * {@code universal_parameters.source} in {@code 002_seed_data.sql}).
 */
class DerivedSourceIndicatorsTest {

    // single bar: open=10, high=20, low=8, close=14
    private static final BarSeries SERIES = BarSeriesFixtures.ohlcvSeries(
            new double[] {10}, new double[] {20}, new double[] {8}, new double[] {14}, new double[] {1});

    @Test
    void hl2_isAverageOfHighAndLow() {
        Num value = new Hl2Indicator(SERIES).getValue(0);
        assertThat(value.doubleValue()).isEqualTo((20 + 8) / 2.0);
    }

    @Test
    void hlc3_isAverageOfHighLowClose() {
        Num value = new Hlc3Indicator(SERIES).getValue(0);
        assertThat(value.doubleValue()).isEqualTo((20 + 8 + 14) / 3.0);
    }

    @Test
    void ohlc4_isAverageOfAllFourPrices() {
        Num value = new Ohlc4Indicator(SERIES).getValue(0);
        assertThat(value.doubleValue()).isEqualTo((10 + 20 + 8 + 14) / 4.0);
    }

    @Test
    void resolve_dispatchesEveryAllowedSourceValue() {
        assertThat(PriceSourceResolver.resolve(SERIES, "open").getValue(0).doubleValue()).isEqualTo(10);
        assertThat(PriceSourceResolver.resolve(SERIES, "high").getValue(0).doubleValue()).isEqualTo(20);
        assertThat(PriceSourceResolver.resolve(SERIES, "low").getValue(0).doubleValue()).isEqualTo(8);
        assertThat(PriceSourceResolver.resolve(SERIES, "close").getValue(0).doubleValue()).isEqualTo(14);
        assertThat(PriceSourceResolver.resolve(SERIES, "hl2")).isInstanceOf(Hl2Indicator.class);
        assertThat(PriceSourceResolver.resolve(SERIES, "hlc3")).isInstanceOf(Hlc3Indicator.class);
        assertThat(PriceSourceResolver.resolve(SERIES, "ohlc4")).isInstanceOf(Ohlc4Indicator.class);
    }

    @Test
    void resolve_unknownSource_throwsBadRequest() {
        assertThatThrownBy(() -> PriceSourceResolver.resolve(SERIES, "vwap"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
