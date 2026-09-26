package com.polypilot.indicator.calc;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Typed, read-only view over one indicator's already-resolved parameter
 * values (raw request values merged with {@code indicator_parameters}
 * defaults — see {@code IndicatorCalculationService.resolveParams}). Every
 * key a registered {@link IndicatorCalculator} asks for is expected to be
 * present by the time it receives this; a missing key means the catalog and
 * the calculator have drifted out of sync, which is a bug, not a user input
 * error — hence the unchecked exception.
 */
@RequiredArgsConstructor
public class IndicatorParams {

    private final Map<String, String> values;

    public String getString(String key) {
        return require(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(require(key));
    }

    public BigDecimal getDecimal(String key) {
        return new BigDecimal(require(key));
    }

    private String require(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "No resolved value for parameter [" + key + "] — catalog/calculator mismatch");
        }
        return value;
    }
}
