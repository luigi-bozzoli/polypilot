package com.polypilot.market.mapper;

import com.polypilot.market.dto.MarketResponseDto;
import com.polypilot.market.enums.MarketStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketStatusResolverTest {

    private final MarketStatusResolver resolver = new MarketStatusResolver();

    @Test
    void umaResolved_isResolved() {
        MarketResponseDto dto = new MarketResponseDto();
        dto.setUmaResolutionStatus("resolved");
        dto.setClosed(true);
        assertThat(resolver.resolve(dto)).isEqualTo(MarketStatus.RESOLVED);
    }

    @Test
    void closedButNotResolved_isClosed() {
        MarketResponseDto dto = new MarketResponseDto();
        dto.setClosed(true);
        assertThat(resolver.resolve(dto)).isEqualTo(MarketStatus.CLOSED);
    }

    @Test
    void openMarket_isOpen_evenWithNoStatusFields() {
        MarketResponseDto dto = new MarketResponseDto();
        assertThat(resolver.resolve(dto)).isEqualTo(MarketStatus.OPEN);

        dto.setClosed(false);
        assertThat(resolver.resolve(dto)).isEqualTo(MarketStatus.OPEN);
    }
}
