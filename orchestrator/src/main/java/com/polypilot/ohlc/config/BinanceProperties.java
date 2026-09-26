package com.polypilot.ohlc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

/**
 * Config for the Binance OHLC candle-sync job (the {@code ohlc-sync} scheduled
 * task). All values are {@code ${ENV:default}} in {@code application.yml}; the
 * defaults are production-correct so nothing has to be set for a normal run.
 *
 * <p>Which symbols and timeframes are synced is <em>not</em> here — it comes
 * from the DB-backed {@code ticker} / {@code timeframe} tables (see
 * {@code ReferenceDataService}).
 *
 * <p>Picked up automatically by {@code @ConfigurationPropertiesScan} on
 * {@code PolyPilotApplication}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {

    /** Binance Spot REST base URL. */
    @NotBlank
    private String baseUrl = "https://api.binance.com";

    /** Candles pulled per (symbol, timeframe) on the first sync (cold start). */
    @Min(1)
    private int backfillCandles = 1000;

    /** Attempts for a single Binance call before it is allowed to fail. */
    @Min(1)
    private int maxRetries = 3;

    /** Base wait for the retry backoff when Binance sends no {@code Retry-After}. */
    private Duration retryBackoff = Duration.ofSeconds(1);
}
