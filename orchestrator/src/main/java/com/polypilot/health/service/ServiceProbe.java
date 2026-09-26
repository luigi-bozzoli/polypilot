package com.polypilot.health.service;

import com.polypilot.health.support.ProbeFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Probes a downstream service's {@code /health}, timing the round trip.
 *
 * <p>On success the downstream body is passed through verbatim with the
 * measured {@code latencyMs} merged into its {@code metrics} object; on a
 * network / non-2xx failure a {@code {status: "unreachable", error: <category>}}
 * stub is returned so the aggregate {@code GET /health} never fails as a whole.
 * The {@code error} value is a fixed category token (never a raw exception
 * message) so internal hostnames / ports don't leak into the response; the full
 * exception is logged at WARN.
 *
 * <p>Unexpected exceptions (bugs in this method's own logic) are <em>not</em>
 * caught — they propagate rather than being misreported as "unreachable".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceProbe {

    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Qualifier("healthProbeRestClient")
    private final RestClient restClient;


    public Map<String, Object> probe(String url) {
        long startNanos = System.nanoTime();
        try {
            Map<String, Object> body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RESPONSE_TYPE);
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

            Map<String, Object> result = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
            result.put(ProbeFields.METRICS, withLatency(result.get(ProbeFields.METRICS), latencyMs));
            return result;
        } catch (RestClientResponseException ex) {
            log.warn("Health probe to [{}] failed", url, ex);
            return errorStub(ProbeFields.ERR_HTTP);
        } catch (ResourceAccessException ex) {
            log.warn("Health probe to [{}] failed", url, ex);
            return errorStub(categorize(ex));
        } catch (RestClientException ex) {
            log.warn("Health probe to [{}] failed", url, ex);
            return errorStub(ProbeFields.ERR_PROBE_FAILED);
        }
    }

    /**
     * Map an I/O failure to a coarse category, without exposing the underlying message.
     */
    private String categorize(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        // Connect timeout and read timeout both surface as HttpTimeoutException
        // (HttpConnectTimeoutException is a subtype); check before ConnectException.
        if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
            return ProbeFields.ERR_TIMEOUT;
        }
        // A refused / unroutable connection — distinct from a slow one.
        if (cause instanceof ConnectException) {
            return ProbeFields.ERR_CONNECTION_REFUSED;
        }
        return ProbeFields.ERR_PROBE_FAILED;
    }

    private Map<String, Object> errorStub(String category) {
        return Map.of(ProbeFields.STATUS, ProbeFields.UNREACHABLE, ProbeFields.ERROR, category);
    }

    /**
     * Merge the measured latency into whatever {@code metrics} the downstream reported.
     */
    private Map<String, Object> withLatency(Object reportedMetrics, long latencyMs) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (reportedMetrics instanceof Map<?, ?> map) {
            map.forEach((key, value) -> merged.put(String.valueOf(key), value));
        }
        merged.put(ProbeFields.LATENCY_MS, latencyMs);
        return merged;
    }
}
