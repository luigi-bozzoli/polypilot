package com.polypilot.ohlc.client;

import com.polypilot.ohlc.config.BinanceProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Thin wrapper over the public Binance Spot REST API — klines only, no auth.
 * Reuses the shared {@link RestClient} bean and passes absolute URLs, the same
 * way {@code MarketItemSyncService} calls Gamma.
 *
 * <p>Binance's IP weight budget (6000/min) dwarfs this job's load (~60/min), so
 * there is no rate limiter. There is defensive retry: HTTP 429 / 418 (with
 * {@code Retry-After} honoured), 5xx, and connect/read timeouts are retried with
 * a capped exponential backoff up to {@link BinanceProperties#getMaxRetries()}.
 */
@Slf4j
@Component
@AllArgsConstructor
public class BinanceClient {

    private static final ParameterizedTypeReference<List<List<Object>>> KLINE_ROWS =
            new ParameterizedTypeReference<>() {
            };

    /** Binance hard cap on {@code limit}. */
    public static final int MAX_LIMIT = 1000;

    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final BinanceProperties binanceProperties;

    /**
     * Fetch klines for one {@code (symbol, interval)}. {@code startTimeMs} /
     * {@code endTimeMs} are epoch millis, inclusive, and may be {@code null}.
     * With no {@code startTime} Binance returns the most recent {@code limit}
     * candles.
     */
    public List<BinanceKline> fetchKlines(String symbol,
                                          String interval,
                                          Long startTimeMs,
                                          Long endTimeMs,
                                          int limit) {

        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(binanceProperties.getBaseUrl() + "/api/v3/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval)
                .queryParam("limit", Math.min(limit, MAX_LIMIT));

        if (startTimeMs != null) {
            uri.queryParam("startTime", startTimeMs);
        }
        if (endTimeMs != null) {
            uri.queryParam("endTime", endTimeMs);
        }

        String url = uri.build().toUriString();

        List<List<Object>> rows = executeWithRetry(() -> restClient.get()
                .uri(url)
                .retrieve()
                .body(KLINE_ROWS));

        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(BinanceKline::fromArray).toList();
    }

    private <T> T executeWithRetry(Supplier<T> call) {
        int maxAttempts = binanceProperties.getMaxRetries();
        RuntimeException last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (HttpStatusCodeException ex) {
                if (!isRetryable(ex) || attempt == maxAttempts) {
                    throw ex;
                }
                last = ex;
                sleep(backoffFor(ex, attempt));
            } catch (ResourceAccessException ex) {
                if (attempt == maxAttempts) {
                    throw ex;
                }
                last = ex;
                sleep(backoffFor(null, attempt));
            }
            log.warn("Binance call failed (attempt {}/{}), retrying: {}",
                    attempt, maxAttempts, last.getMessage());
        }
        // Unreachable: the last attempt always either returns or rethrows.
        throw last;
    }

    private static boolean isRetryable(HttpStatusCodeException ex) {
        int status = ex.getStatusCode().value();
        return status == 429                       // rate limited
                || status == 418                   // IP auto-banned for repeated 429s
                || ex instanceof HttpServerErrorException;  // 5xx
    }

    private Duration backoffFor(HttpStatusCodeException ex, int attempt) {
        if (ex != null) {
            Duration retryAfter = parseRetryAfter(ex.getResponseHeaders());
            if (retryAfter != null) {
                return min(retryAfter, MAX_BACKOFF);
            }
        }
        Duration exp = binanceProperties.getRetryBackoff().multipliedBy(1L << (attempt - 1));
        return min(exp, MAX_BACKOFF);
    }

    private static Duration parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return null;  // HTTP-date form is not used by Binance; ignore it
        }
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off a Binance retry", ex);
        }
    }
}
