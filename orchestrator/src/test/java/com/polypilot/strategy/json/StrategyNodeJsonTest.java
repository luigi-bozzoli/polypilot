package com.polypilot.strategy.json;

import com.polypilot.enums.SentimentType;
import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips every {@link StrategyNode} discriminator through {@link StrategyNodeSerializer}'s
 * wire format, and pins the exact property names the frontend
 * (`dashboard/src/features/strategies/types.ts`) expects.
 */
class StrategyNodeJsonTest {

    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder()
                .addModule(new StrategyNodeJacksonModule().strategyNodeModule())
                .build();
    }

    @Test
    void indicatorNode_roundTrips_andUsesTheContractPropertyNames() {
        CompareNode node = new CompareNode(
                new IndicatorOperand(new IndicatorRequestKey("rsi", Map.of("period", "14")), "value"),
                CompareOperator.GT, new LiteralOperand(BigDecimal.valueOf(70)));

        String json = mapper.writeValueAsString(node);
        JsonNode tree = mapper.readTree(json);

        assertThat(tree.get("type").asText()).isEqualTo("INDICATOR");
        assertThat(tree.get("indicatorKey").asText()).isEqualTo("rsi");
        assertThat(tree.get("params").get("period").asText()).isEqualTo("14");
        assertThat(tree.get("outputField").asText()).isEqualTo("value");
        assertThat(tree.get("operator").asText()).isEqualTo("GT");
        assertThat(tree.get("value").decimalValue()).isEqualByComparingTo("70");
        assertThat(tree.has("threshold")).isFalse();
        assertThat(tree.has("indicator")).isFalse();

        CompareNode roundTripped = (CompareNode) mapper.readValue(json, StrategyNode.class);
        IndicatorOperand roundTrippedLeft = (IndicatorOperand) roundTripped.getLeft();
        IndicatorOperand originalLeft = (IndicatorOperand) node.getLeft();
        assertThat(roundTrippedLeft.getIndicator()).isEqualTo(originalLeft.getIndicator());
        assertThat(roundTrippedLeft.getOutputField()).isEqualTo(originalLeft.getOutputField());
        assertThat(roundTripped.getOperator()).isEqualTo(node.getOperator());
        assertThat(((LiteralOperand) roundTripped.getRight()).getValue())
                .isEqualByComparingTo(((LiteralOperand) node.getRight()).getValue());
    }

    @Test
    void indicatorVsIndicatorNode_roundTrips_withANestedValueObject() {
        CompareNode node = new CompareNode(
                new IndicatorOperand(new IndicatorRequestKey("rsi", Map.of("period", "14")), "value"),
                CompareOperator.GT,
                new IndicatorOperand(new IndicatorRequestKey("macd", Map.of("fast_period", "12")), "signal"));

        String json = mapper.writeValueAsString(node);
        JsonNode tree = mapper.readTree(json);

        assertThat(tree.get("type").asText()).isEqualTo("INDICATOR");
        assertThat(tree.get("indicatorKey").asText()).isEqualTo("rsi");
        assertThat(tree.get("value").get("indicatorKey").asText()).isEqualTo("macd");
        assertThat(tree.get("value").get("params").get("fast_period").asText()).isEqualTo("12");
        assertThat(tree.get("value").get("outputField").asText()).isEqualTo("signal");

        CompareNode roundTripped = (CompareNode) mapper.readValue(json, StrategyNode.class);
        IndicatorOperand roundTrippedRight = (IndicatorOperand) roundTripped.getRight();
        assertThat(roundTrippedRight.getIndicator())
                .isEqualTo(new IndicatorRequestKey("macd", Map.of("fast_period", "12")));
        assertThat(roundTrippedRight.getOutputField()).isEqualTo("signal");
    }

    @Test
    void marketFieldNumericNode_roundTrips_andUsesTheContractPropertyNames() {
        MarketFieldCompareNode node = new MarketFieldCompareNode(
                MarketField.UP_PRICE, CompareOperator.GT, new BigDecimal("0.55"), null);

        String json = mapper.writeValueAsString(node);
        JsonNode tree = mapper.readTree(json);

        assertThat(tree.get("type").asText()).isEqualTo("MARKET_FIELD");
        assertThat(tree.get("field").asText()).isEqualTo("up_price");
        assertThat(tree.get("operator").asText()).isEqualTo("GT");
        assertThat(tree.get("value").decimalValue()).isEqualByComparingTo("0.55");

        MarketFieldCompareNode roundTripped = (MarketFieldCompareNode) mapper.readValue(json, StrategyNode.class);
        assertThat(roundTripped.getField()).isEqualTo(MarketField.UP_PRICE);
        assertThat(roundTripped.getOperator()).isEqualTo(CompareOperator.GT);
        assertThat(roundTripped.getThreshold()).isEqualByComparingTo("0.55");
        assertThat(roundTripped.getSentimentValue()).isNull();
    }

    @Test
    void marketFieldSentimentNode_roundTrips_andUsesTheContractPropertyNames() {
        MarketFieldCompareNode node = new MarketFieldCompareNode(
                MarketField.SENTIMENT, CompareOperator.EQ, null, SentimentType.BULLISH);

        String json = mapper.writeValueAsString(node);
        JsonNode tree = mapper.readTree(json);

        assertThat(tree.get("type").asText()).isEqualTo("MARKET_FIELD");
        assertThat(tree.get("field").asText()).isEqualTo("sentiment");
        assertThat(tree.get("operator").asText()).isEqualTo("EQ");
        assertThat(tree.get("value").asText()).isEqualTo("BULLISH");

        MarketFieldCompareNode roundTripped = (MarketFieldCompareNode) mapper.readValue(json, StrategyNode.class);
        assertThat(roundTripped.getField()).isEqualTo(MarketField.SENTIMENT);
        assertThat(roundTripped.getSentimentValue()).isEqualTo(SentimentType.BULLISH);
        assertThat(roundTripped.getThreshold()).isNull();
    }

    @Test
    void marketFieldSentimentDotConfidence_usesTheDottedWireValue() {
        MarketFieldCompareNode node = new MarketFieldCompareNode(
                MarketField.SENTIMENT_CONFIDENCE, CompareOperator.GT, new BigDecimal("0.8"), null);

        JsonNode tree = mapper.readTree(mapper.writeValueAsString(node));

        assertThat(tree.get("field").asText()).isEqualTo("sentiment.confidence");
    }

    @Test
    void booleanNode_roundTrips() {
        BooleanNode tree = new BooleanNode(BooleanOperator.AND,
                new MarketFieldCompareNode(MarketField.UP_PRICE, CompareOperator.LT, new BigDecimal("0.75"), null),
                new MarketFieldCompareNode(MarketField.SENTIMENT, CompareOperator.EQ, null, SentimentType.BULLISH));

        String json = mapper.writeValueAsString(tree);
        JsonNode node = mapper.readTree(json);
        assertThat(node.get("type").asText()).isEqualTo("BOOLEAN");
        assertThat(node.get("operator").asText()).isEqualTo("AND");
        assertThat(node.get("left").get("type").asText()).isEqualTo("MARKET_FIELD");
        assertThat(node.get("right").get("type").asText()).isEqualTo("MARKET_FIELD");

        BooleanNode roundTripped = (BooleanNode) mapper.readValue(json, StrategyNode.class);
        assertThat(roundTripped.getOperator()).isEqualTo(BooleanOperator.AND);
        assertThat(roundTripped.getLeft()).isInstanceOf(MarketFieldCompareNode.class);
        assertThat(roundTripped.getRight()).isInstanceOf(MarketFieldCompareNode.class);
    }

    @Test
    void unaryBooleanNode_roundTrips() {
        UnaryBooleanNode tree = new UnaryBooleanNode(UnaryOperator.NOT,
                new MarketFieldCompareNode(MarketField.UP_PRICE, CompareOperator.LT, new BigDecimal("0.75"), null));

        String json = mapper.writeValueAsString(tree);
        JsonNode node = mapper.readTree(json);
        assertThat(node.get("type").asText()).isEqualTo("UNARY_BOOLEAN");
        assertThat(node.get("operator").asText()).isEqualTo("NOT");
        assertThat(node.get("child").get("type").asText()).isEqualTo("MARKET_FIELD");

        UnaryBooleanNode roundTripped = (UnaryBooleanNode) mapper.readValue(json, StrategyNode.class);
        assertThat(roundTripped.getOperator()).isEqualTo(UnaryOperator.NOT);
        assertThat(roundTripped.getChild()).isInstanceOf(MarketFieldCompareNode.class);
    }

    @Test
    void nestedTree_fromTheContractExample_roundTrips() {
        StrategyNode tree = new BooleanNode(BooleanOperator.AND,
                new BooleanNode(BooleanOperator.AND,
                        new BooleanNode(BooleanOperator.AND,
                                new MarketFieldCompareNode(MarketField.UP_PRICE, CompareOperator.LT, new BigDecimal("0.75"), null),
                                new MarketFieldCompareNode(MarketField.SENTIMENT, CompareOperator.EQ, null, SentimentType.BULLISH)),
                        new MarketFieldCompareNode(MarketField.SENTIMENT_CONFIDENCE, CompareOperator.GT, new BigDecimal("0.8"), null)),
                new MarketFieldCompareNode(MarketField.VOLUME_24H, CompareOperator.GT, new BigDecimal("100000"), null));

        String json = mapper.writeValueAsString(tree);
        StrategyNode roundTripped = mapper.readValue(json, StrategyNode.class);

        assertThat(mapper.writeValueAsString(roundTripped)).isEqualTo(json);
    }
}
