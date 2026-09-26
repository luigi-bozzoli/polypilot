package com.polypilot.health.support;

/**
 * Field names and value tokens used in the JSON a {@code ServiceProbe} emits for
 * a downstream service entry in {@code GET /health}.
 *
 * <p>Kept as named constants because these strings form a contract read back by
 * the dashboard ({@code dashboard/src/features/health/statusTone.ts} keys on
 * {@code status == "unreachable"}) and by any future aggregator.
 */
public final class ProbeFields {

    private ProbeFields() {
    }

    /** Top-level status key on a service entry. */
    public static final String STATUS = "status";
    /** Metrics object key, holding {@link #LATENCY_MS} among the downstream's own fields. */
    public static final String METRICS = "metrics";
    /** Measured probe round-trip, in milliseconds, inside {@link #METRICS}. */
    public static final String LATENCY_MS = "latencyMs";
    /** Diagnostic key on the error stub; carries one of the {@code ERR_*} category tokens. */
    public static final String ERROR = "error";

    /** {@link #STATUS} value when the probe could not obtain a healthy response. */
    public static final String UNREACHABLE = "unreachable";

    /** {@link #ERROR} token: the downstream did not answer within the read timeout. */
    public static final String ERR_TIMEOUT = "timeout";
    /** {@link #ERROR} token: the connection was refused / could not be established. */
    public static final String ERR_CONNECTION_REFUSED = "connection-refused";
    /** {@link #ERROR} token: the downstream answered with a non-2xx status. */
    public static final String ERR_HTTP = "http-error";
    /** {@link #ERROR} token: any other client/transport failure. */
    public static final String ERR_PROBE_FAILED = "probe-failed";
}
