package com.polypilot.indicator.calc.source;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.Num;

/**
 * Resolves the {@code source} universal parameter (see {@code 002_seed_data.sql}
 * section 6a: {@code open, high, low, close, hl2, hlc3, ohlc4}) to the TA4J
 * price {@link Indicator} it names. Stateless — takes the series per call so
 * it needs no Spring bean lifecycle.
 */
public final class PriceSourceResolver {

    private PriceSourceResolver() {
    }

    public static Indicator<Num> resolve(BarSeries series, String source) {
        return switch (source) {
            case "open" -> new OpenPriceIndicator(series);
            case "high" -> new HighPriceIndicator(series);
            case "low" -> new LowPriceIndicator(series);
            case "close" -> new ClosePriceIndicator(series);
            case "hl2" -> new Hl2Indicator(series);
            case "hlc3" -> new Hlc3Indicator(series);
            case "ohlc4" -> new Ohlc4Indicator(series);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown source: " + source);
        };
    }
}
