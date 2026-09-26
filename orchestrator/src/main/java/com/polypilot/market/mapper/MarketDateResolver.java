package com.polypilot.market.mapper;

import com.polypilot.market.dto.MarketResponseDto;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Derives {@code Market.resolutionDate}. Gamma has no single "resolution date"
 * field on a market, so we take the first of, in order:
 * <ol>
 *   <li>{@code closedTime} — the actual close, but in a non-ISO shape
 *       ({@code "2026-08-30 14:55:52+00"})</li>
 *   <li>{@code umaEndDate} — UMA oracle finalization, clean ISO-8601</li>
 *   <li>{@code endDate} — the scheduled settlement time (effectively always present)</li>
 * </ol>
 * So an open market carries its scheduled end, and a resolved one is upgraded to
 * the real close on the next sync.
 */
@Component
public class MarketDateResolver {

    /** Gamma's market-level {@code closedTime}, e.g. {@code "2026-08-30 14:55:52+00"}. */
    private static final DateTimeFormatter CLOSED_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendOffsetId().optionalEnd()
            .toFormatter();

    @Named("resolutionDate")
    public OffsetDateTime resolutionDate(MarketResponseDto dto) {
        OffsetDateTime fromClosedTime = parseFlexible(dto.getClosedTime());
        if (fromClosedTime != null) {
            return fromClosedTime;
        }

        OffsetDateTime fromUma = parseIso(dto.getUmaEndDate());
        if (fromUma != null) {
            return fromUma;
        }

        return dto.getEndDate() == null ? null : dto.getEndDate().atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime parseFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw, CLOSED_TIME);
        } catch (DateTimeParseException withOffset) {
            try {
                return LocalDateTime.parse(raw, CLOSED_TIME).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException withoutOffset) {
                return null;
            }
        }
    }

    private static OffsetDateTime parseIso(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException notOffsetDateTime) {
            try {
                return Instant.parse(raw).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException notInstant) {
                return null;
            }
        }
    }
}
