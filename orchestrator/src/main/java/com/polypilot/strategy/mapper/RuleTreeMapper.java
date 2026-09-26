package com.polypilot.strategy.mapper;

import com.polypilot.strategy.StrategyNode;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Parses {@code strategies.rule_tree} (stored as a raw JSON string on
 * {@link com.polypilot.entity.Strategy#getRuleTree()}) into a {@link StrategyNode} tree, using the
 * app's {@link ObjectMapper} so {@link com.polypilot.strategy.json.StrategyNodeJacksonModule}'s
 * deserializer applies. Mirrors {@link com.polypilot.market.mapper.OutcomePricesMapper}'s
 * shape: a small handwritten {@code @Component} pulled into a MapStruct mapper via {@code uses},
 * since the JSON parse needs a dependency MapStruct can't generate.
 */
@Slf4j
@Component
public class RuleTreeMapper {

    private final ObjectMapper objectMapper;

    public RuleTreeMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("parseRuleTree")
    public StrategyNode parseRuleTree(String ruleTree) {
        if (ruleTree == null || ruleTree.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(ruleTree, StrategyNode.class);
        } catch (JacksonException | IllegalArgumentException e) {
            log.error("Malformed rule_tree JSON", e);
            return null;
        }
    }

    /**
     * The inverse of {@link #parseRuleTree}, for {@code POST /strategies}: serializes a
     * {@link StrategyNode} tree built from a create request back into the JSON string
     * {@link com.polypilot.entity.Strategy#getRuleTree()} stores, via
     * {@link com.polypilot.strategy.json.StrategyNodeSerializer} (registered on the same
     * {@link ObjectMapper}).
     */
    public String serializeRuleTree(StrategyNode ruleTree) {
        return objectMapper.writeValueAsString(ruleTree);
    }
}
