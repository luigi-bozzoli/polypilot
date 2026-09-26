package com.polypilot.market.mapper;

import com.polypilot.market.dto.MarketResponseDto;
import com.polypilot.market.enums.MarketStatus;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class MarketStatusResolver {

    @Named("resolve")
    public MarketStatus resolve(MarketResponseDto dto) {
        if ("resolved".equalsIgnoreCase(dto.getUmaResolutionStatus())) {
            return MarketStatus.RESOLVED;
        }

        if (Boolean.TRUE.equals(dto.getClosed())) {
            return MarketStatus.CLOSED;
        }

        return MarketStatus.OPEN;
    }
}