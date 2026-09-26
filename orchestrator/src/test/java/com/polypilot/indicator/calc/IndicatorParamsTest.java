package com.polypilot.indicator.calc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndicatorParamsTest {

    @Test
    void typedGetters_parseResolvedValues() {
        IndicatorParams params = new IndicatorParams(Map.of(
                "period", "14",
                "source", "close",
                "overbought_level", "70.5"));

        assertThat(params.getInt("period")).isEqualTo(14);
        assertThat(params.getString("source")).isEqualTo("close");
        assertThat(params.getDecimal("overbought_level")).isEqualByComparingTo(new BigDecimal("70.5"));
    }

    @Test
    void missingKey_throwsIllegalState_notNullPointer() {
        IndicatorParams params = new IndicatorParams(Map.of());

        assertThatThrownBy(() -> params.getInt("period"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("period");
    }
}
