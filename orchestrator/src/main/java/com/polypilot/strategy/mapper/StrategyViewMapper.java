package com.polypilot.strategy.mapper;

import com.polypilot.entity.Strategy;
import com.polypilot.strategy.dto.view.StrategyView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Entity &rarr; outbound view mapping for {@link Strategy}. {@code ruleTree} goes through
 * {@link RuleTreeMapper} (see that class) since MapStruct can't generate the JSON parse itself.
 */
@Mapper(componentModel = "spring", uses = RuleTreeMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StrategyViewMapper {

    List<StrategyView> toViews(List<Strategy> strategies);

    @Mapping(target = "ruleTree", qualifiedByName = "parseRuleTree")
    @Mapping(target = "seriesId", source = "series.id")
    @Mapping(target = "seriesTitle", source = "series.title")
    StrategyView toView(Strategy strategy);
}
