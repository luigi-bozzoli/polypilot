package com.polypilot.health.support;

import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Formats a process uptime into the short, display-ready shape the health
 * dashboard expects:
 * {@code "3d 04h"} once past a day, {@code "4h 12m"} within a day,
 * {@code "3m 20s"} within an hour, {@code "12s"} below a minute.
 */
public final class UptimeFormatter {

    public static String format(Duration uptime) {
        long totalSeconds = Math.max(0, uptime.getSeconds());
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return "%dd %02dh".formatted(days, hours);
        }
        if (hours > 0) {
            return "%dh %02dm".formatted(hours, minutes);
        }
        if (minutes > 0) {
            return "%dm %02ds".formatted(minutes, seconds);
        }
        return "%ds".formatted(seconds);
    }
}
