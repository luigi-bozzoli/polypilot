package com.polypilot.strategy.json;

import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
import com.polypilot.strategy.ComparisonOperand;
import com.polypilot.strategy.IndicatorOperand;
import com.polypilot.strategy.LiteralOperand;
import com.polypilot.strategy.MarketFieldCompareNode;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.UnaryBooleanNode;
import com.polypilot.strategy.enums.MarketField;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Writes a {@link StrategyNode} tree in this rule-tree's wire format: a discriminated
 * {@code "type"} property (@{code BOOLEAN}/{@code UNARY_BOOLEAN}/{@code INDICATOR}/
 * {@code MARKET_FIELD}) on an otherwise flat object.
 *
 * <p>Fully manual rather than {@code @JsonTypeInfo}: {@link CompareNode} and
 * {@link MarketFieldCompareNode}'s wire shape doesn't match their Java field names 1:1 (the
 * nested {@code IndicatorRequestKey} flattens into {@code indicatorKey}/{@code params}, and
 * {@code threshold}/{@code sentimentValue} both collapse into one {@code value} property), so a
 * type-info wrapper over default bean serialization can't produce it — this writes every property
 * itself. Registered for {@link StrategyNode} and each concrete subtype in
 * {@link StrategyNodeJacksonModule} so it's used both for the tree root and for every nested
 * {@code left}/{@code right}/{@code child} field.
 */
public class StrategyNodeSerializer extends ValueSerializer<StrategyNode> {

    @Override
    public void serialize(StrategyNode value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        switch (value) {
            case BooleanNode node -> writeBoolean(node, gen, ctxt);
            case UnaryBooleanNode node -> writeUnaryBoolean(node, gen, ctxt);
            case CompareNode node -> writeIndicator(node, gen, ctxt);
            case MarketFieldCompareNode node -> writeMarketField(node, gen, ctxt);
            default -> throw new IllegalArgumentException("Unknown StrategyNode subtype: " + value.getClass());
        }
    }

    private void writeBoolean(BooleanNode node, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("type", "BOOLEAN");
        gen.writeStringProperty("operator", node.getOperator().name());
        gen.writeName("left");
        serialize(node.getLeft(), gen, ctxt);
        gen.writeName("right");
        serialize(node.getRight(), gen, ctxt);
        gen.writeEndObject();
    }

    private void writeUnaryBoolean(UnaryBooleanNode node, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("type", "UNARY_BOOLEAN");
        gen.writeStringProperty("operator", node.getOperator().name());
        gen.writeName("child");
        serialize(node.getChild(), gen, ctxt);
        gen.writeEndObject();
    }

    private void writeIndicator(CompareNode node, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        IndicatorOperand left = (IndicatorOperand) node.getLeft();

        gen.writeStartObject();
        gen.writeStringProperty("type", "INDICATOR");
        gen.writeStringProperty("indicatorKey", left.getIndicator().getIndicatorKey());
        gen.writeName("params");
        ctxt.writeValue(gen, left.getIndicator().getParams());
        gen.writeStringProperty("outputField", left.getOutputField());
        gen.writeStringProperty("operator", node.getOperator().name());
        gen.writeName("value");
        writeOperandValue(node.getRight(), gen, ctxt);
        gen.writeEndObject();
    }

    /**
     * The {@code value} property is polymorphic: a bare number for a {@link LiteralOperand}
     * (unchanged from before indicator-vs-indicator comparisons existed, so every already-persisted
     * {@code rule_tree} keeps parsing identically), or a nested {@code {indicatorKey, params,
     * outputField}} object for an {@link IndicatorOperand} right-hand side.
     */
    private void writeOperandValue(ComparisonOperand operand, JsonGenerator gen, SerializationContext ctxt)
            throws JacksonException {
        if (operand instanceof LiteralOperand literal) {
            ctxt.writeValue(gen, literal.getValue());
        } else if (operand instanceof IndicatorOperand indicatorOperand) {
            gen.writeStartObject();
            gen.writeStringProperty("indicatorKey", indicatorOperand.getIndicator().getIndicatorKey());
            gen.writeName("params");
            ctxt.writeValue(gen, indicatorOperand.getIndicator().getParams());
            gen.writeStringProperty("outputField", indicatorOperand.getOutputField());
            gen.writeEndObject();
        } else {
            throw new IllegalArgumentException("Unknown ComparisonOperand type: " + operand.getClass());
        }
    }

    private void writeMarketField(MarketFieldCompareNode node, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("type", "MARKET_FIELD");
        gen.writeName("field");
        ctxt.writeValue(gen, node.getField());
        gen.writeStringProperty("operator", node.getOperator().name());
        gen.writeName("value");
        if (node.getField() == MarketField.SENTIMENT) {
            gen.writeString(node.getSentimentValue().name());
        } else {
            ctxt.writeValue(gen, node.getThreshold());
        }
        gen.writeEndObject();
    }
}
