package com.polypilot.health.support;

import lombok.NoArgsConstructor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;

/**
 * Thin read-only view over the JVM's own management beans, used to fill the
 * orchestrator's self-reported {@code metrics} block on {@code GET /health}.
 */
@NoArgsConstructor
public final class JvmMetrics {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();


    /** Wall-clock time since this JVM started. */
    public static Duration uptime() {
        return Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
    }

    /**
     * Heap utilisation as a single fragment, e.g. {@code "heap 61%"}. Uses the
     * configured max when known, falling back to the currently committed size.
     */
    public static String heapDetail() {
        var heap = MEMORY.getHeapMemoryUsage();
        long used = heap.getUsed();
        long ceiling = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
        long pct = ceiling > 0 ? Math.round(100.0 * used / ceiling) : 0;
        return "heap " + pct + "%";
    }
}
