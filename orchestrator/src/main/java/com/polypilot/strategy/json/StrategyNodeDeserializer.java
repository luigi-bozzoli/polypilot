package com.polypilot.strategy.json;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
import com.polypilot.strategy.ComparisonOperand;
import com.polypilot.strategy.IndicatorOperand;
import com.polypilot.strategy.IndicatorRequestKey;
import com.polypilot.strategy.LiteralOperand;
import com.polypilot.strategy.MarketFieldCompareNode;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.UnaryBooleanNode;
import com.polypilot.strategy.enums.BooleanOperator;
import com.polypilot.strategy.enums.CompareOperator;
import com.polypilot.strategy.enums.MarketField;
import com.polypilot.strategy.enums.UnaryOperator;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads the wire format {@link StrategyNodeSerializer} writes back into a {@link StrategyNode}
 * tree. See that class for why this is fully manual rather than {@code @JsonTypeInfo}-driven.
 *
 * <p>Reads the whole node as a {@link JsonNode} first (rather than streaming token-by-token) —
 * trees are small (a sane node-count cap is a future {@code POST /strategies} validation concern,
 * not this deserializer's), and a tree makes the {@code INDICATOR}/{@code MARKET_FIELD} field
 * flattening straightforward to unpack in any order.
 */
public class StrategyNodeDeserializer extends ValueDeserializer<StrategyNode> {

    @Override
    public StrategyNode deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return fromTree(ctxt.readTree(p), ctxt);
    }

    private StrategyNode fromTree(JsonNode node, DeserializationContext ctxt) throws JacksonException {
        String type = textField(node, "type");
        return switch (type) {
            case "BOOLEAN" -> new BooleanNode(
                    BooleanOperator.valueOf(textField(node, "operator")),
                    fromTree(node.get("left"), ctxt),
                    fromTree(node.get("right"), ctxt));
            case "UNARY_BOOLEAN" -> new UnaryBooleanNode(
                    UnaryOperator.valueOf(textField(node, "operator")),
                    fromTree(node.get("child"), ctxt));
            case "INDICATOR" -> readIndicator(node);
            case "MARKET_FIELD" -> readMarketField(node, ctxt);
            default -> throw new IllegalArgumentException("Unknown StrategyNode type: " + type);
        };
    }

    private CompareNode readIndicator(JsonNode node) {
        IndicatorRequestKey key = new IndicatorRequestKey(textField(node, "indicatorKey"), readParams(node.get("params")));
        IndicatorOperand left = new IndicatorOperand(key, textField(node, "outputField"));
        return new CompareNode(
                left,
                CompareOperator.valueOf(textField(node, "operator")),
                readOperandValue(node.get("value")));
    }

    /**
     * The {@code value} property is polymorphic: a bare number is a {@link LiteralOperand} (the
     * original, still-supported shape — every already-persisted {@code rule_tree} parses
     * unchanged), a nested {@code {indicatorKey, params, outputField}} object is an
     * {@link IndicatorOperand} right-hand side.
     */
    private ComparisonOperand readOperandValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull() || valueNode.isMissingNode()) {
            throw new IllegalArgumentException("Missing required field [value] on StrategyNode JSON");
        }
        if (valueNode.isObject()) {
            IndicatorRequestKey key = new IndicatorRequestKey(
                    textField(valueNode, "indicatorKey"), readParams(valueNode.get("params")));
            return new IndicatorOperand(key, textField(valueNode, "outputField"));
        }
        return new LiteralOperand(valueNode.decimalValue());
    }

    private MarketFieldCompareNode readMarketField(JsonNode node, DeserializationContext ctxt) throws JacksonException {
        MarketField field = ctxt.readTreeAsValue(node.get("field"), MarketField.class);
        CompareOperator operator = CompareOperator.valueOf(textField(node, "operator"));
        JsonNode valueNode = node.get("value");

        if (field == MarketField.SENTIMENT) {
            return new MarketFieldCompareNode(field, operator, null, SentimentType.valueOf(valueNode.asString()));
        }
        return new MarketFieldCompareNode(field, operator, valueNode.decimalValue(), null);
    }

    private Map<String, String> readParams(JsonNode paramsNode) {
        Map<String, String> params = new HashMap<>();
        if (paramsNode == null || paramsNode.isNull() || paramsNode.isMissingNode()) {
            return params;
        }
        for (Map.Entry<String, JsonNode> entry : paramsNode.properties()) {
            params.put(entry.getKey(), entry.getValue().asString());
        }
        return params;
    }

    private String textField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            throw new IllegalArgumentException("Missing required field [" + field + "] on StrategyNode JSON");
        }
        return value.asString();
    }
}
