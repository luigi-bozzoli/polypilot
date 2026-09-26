package com.polypilot.market.mapper;


import com.polypilot.market.dto.MarketResponseDto;
import com.polypilot.market.entity.Market;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {OutcomePricesMapper.class, MarketStatusResolver.class, MarketDateResolver.class}
)
public interface MarketMapper {

    @Mapping(target = "polymarketConditionId", source = "conditionId")
    @Mapping(target = "volume24h", source = "volume24hr")
    @Mapping(target = "liquidity", source = "liquidityNum")
    @Mapping(
            target = "upPrice",
            source = "outcomePrices",
            qualifiedByName = "upPrice"
    )
    @Mapping(
            target = "downPrice",
            source = "outcomePrices",
            qualifiedByName = "downPrice"
    )
    @Mapping(
            target = "outcome",
            source = "outcomePrices",
            qualifiedByName = "marketOutcome"
    )
    @Mapping(target = "status", source = ".", qualifiedByName = "resolve")
    @Mapping(target = "resolutionDate", source = ".", qualifiedByName = "resolutionDate")
    Market toEntity(MarketResponseDto dto);

    List<Market> toEntities(List<MarketResponseDto> dtos);

    /**
     * In-place refresh of an existing row. {@code IGNORE} is deliberate: Gamma
     * drops {@code volume24hr}/{@code liquidityNum}/{@code outcomePrices} from the
     * payload once a market resolves, and without this a refresh would null out
     * the last good values instead of keeping them.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "polymarketConditionId", source = "conditionId")
    @Mapping(target = "volume24h", source = "volume24hr")
    @Mapping(target = "liquidity", source = "liquidityNum")
    @Mapping(
            target = "upPrice",
            source = "outcomePrices",
            qualifiedByName = "upPrice"
    )
    @Mapping(
            target = "downPrice",
            source = "outcomePrices",
            qualifiedByName = "downPrice"
    )
    @Mapping(
            target = "outcome",
            source = "outcomePrices",
            qualifiedByName = "marketOutcome"
    )
    @Mapping(target = "status", source = ".", qualifiedByName = "resolve")
    @Mapping(target = "resolutionDate", source = ".", qualifiedByName = "resolutionDate")
    void updateEntity(
            MarketResponseDto dto,
            @MappingTarget Market market
    );
}