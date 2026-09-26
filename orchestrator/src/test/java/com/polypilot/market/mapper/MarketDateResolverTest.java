package com.polypilot.market.mapper;

import com.polypilot.market.dto.MarketResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDateResolverTest {

    private final MarketDateResolver resolver = new MarketDateResolver();

    @Test
    void prefersClosedTime_inGammasNonIsoShape() {
        MarketResponseDto dto = new MarketResponseDto();
        dto.setClosedTime("2026-08-30 14:55:52+00");
        dto.setUmaEndDate("2026-08-30T14:55:52Z");
        dto.setEndDate(Instant.parse("2026-08-30T14:55:00Z"));

        assertThat(resolver.resolutionDate(dto))
                .isEqualTo(OffsetDateTime.of(2026, 8, 30, 14, 55, 52, 0, ZoneOffset.UTC));
    }

    @Test
    void fallsBackToUmaEndDate_whenClosedTimeMissing() {
        MarketResponseDto dto = new MarketResponseDto();
        dto.setUmaEndDate("2026-08-30T14:55:52Z");
        dto.setEndDate(Instant.parse("2026-08-30T14:55:00Z"));

        assertThat(resolver.resolutionDate(dto))
                .isEqualTo(OffsetDateTime.of(2026, 8, 30, 14, 55, 52, 0, ZoneOffset.UTC));
    }

    @Test
    void fallsBackToScheduledEndDate_forAnOpenMarket() {
        MarketResponseDto dto = new MarketResponseDto();
        dto.setEndDate(Instant.parse("2026-09-06T14:05:00Z"));

        assertThat(resolver.resolutionDate(dto))
                .isEqualTo(OffsetDateTime.of(2026, 9, 6, 14, 5, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void nullWhenNothingIsAvailable() {
        assertThat(resolver.resolutionDate(new MarketResponseDto())).isNull();
    }

    @Test
    void ignoresUnparseableClosedTimeAndMovesOn() {
        MarketResponseDto dto = new MarketResponseDto();
        dto.setClosedTime("not a date");
        dto.setEndDate(Instant.parse("2026-09-06T14:05:00Z"));

        assertThat(resolver.resolutionDate(dto))
                .isEqualTo(OffsetDateTime.of(2026, 9, 6, 14, 5, 0, 0, ZoneOffset.UTC));
    }
}
