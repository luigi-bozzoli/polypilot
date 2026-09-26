package com.polypilot.strategy.mapper;

import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
import com.polypilot.strategy.IndicatorOperand;
import com.polypilot.strategy.IndicatorRequestKey;
import com.polypilot.strategy.LiteralOperand;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.enums.BooleanOperator;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.json.StrategyNodeJacksonModule;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleTreeMapperTest {

    private final RuleTreeMapper mapper = new RuleTreeMapper(
            JsonMapper.builder().addModule(new StrategyNodeJacksonModule().strategyNodeModule()).build());

    @Test
    void parsesANestedTreeIntoStrategyNodes() {
        String json = """
                {"type":"BOOLEAN","operator":"AND",
                  "left":{"type":"INDICATOR","indicatorKey":"rsi","params":{"period":"14"},
                           "outputField":"value","operator":"GT","value":70},
                  "right":{"type":"INDICATOR","indicatorKey":"rsi","params":{"period":"14"},
                            "outputField":"value","operator":"LT","value":80}}
                """;

        StrategyNode result = mapper.parseRuleTree(json);

        assertThat(result).isInstanceOf(BooleanNode.class);
        BooleanNode root = (BooleanNode) result;
        assertThat(root.getOperator()).isEqualTo(BooleanOperator.AND);
        assertThat(root.getLeft()).isInstanceOf(CompareNode.class);

        CompareNode left = (CompareNode) root.getLeft();
        IndicatorOperand leftOperand = (IndicatorOperand) left.getLeft();
        assertThat(leftOperand.getIndicator()).isEqualTo(new IndicatorRequestKey("rsi", Map.of("period", "14")));
        assertThat(left.getOperator()).isEqualTo(CompareOperator.GT);
        assertThat(((LiteralOperand) left.getRight()).getValue()).isEqualByComparingTo("70");
    }

    @Test
    void missingOrBlankRuleTree_yieldsNullNotException() {
        assertThat(mapper.parseRuleTree(null)).isNull();
        assertThat(mapper.parseRuleTree("")).isNull();
        assertThat(mapper.parseRuleTree("   ")).isNull();
    }

    @Test
    void malformedJson_yieldsNullNotException() {
        assertThat(mapper.parseRuleTree("not-json")).isNull();
    }

    @Test
    void legacyPreContractShape_yieldsNullNotException() {
        // The shape 001_schema.sql's own comment documents (operator/conditions/op/field/value) —
        // pre-dates the type-discriminated wire format and lacks the required "type" field, which
        // StrategyNodeDeserializer rejects with an IllegalArgumentException rather than a
        // JacksonException.
        String legacy = """
                {"operator": "AND", "conditions": [
                  {"op": "lt", "field": "up_price", "value": 0.30}
                ]}
                """;

        assertThat(mapper.parseRuleTree(legacy)).isNull();
    }

    @Test
    void serializeThenParse_roundTrips() {
        StrategyNode tree = new BooleanNode(BooleanOperator.AND,
                new CompareNode(new IndicatorOperand(new IndicatorRequestKey("rsi", Map.of("period", "14")), "value"),
                        CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70))),
                new CompareNode(new IndicatorOperand(new IndicatorRequestKey("rsi", Map.of("period", "14")), "value"),
                        CompareOperator.LT, new LiteralOperand(BigDecimal.valueOf(80))));

        String json = mapper.serializeRuleTree(tree);
        StrategyNode parsed = mapper.parseRuleTree(json);

        assertThat(parsed).isInstanceOf(BooleanNode.class);
        BooleanNode root = (BooleanNode) parsed;
        assertThat(root.getOperator()).isEqualTo(BooleanOperator.AND);
        assertThat(((IndicatorOperand) ((CompareNode) root.getLeft()).getLeft()).getIndicator())
                .isEqualTo(new IndicatorRequestKey("rsi", Map.of("period", "14")));
    }
}
