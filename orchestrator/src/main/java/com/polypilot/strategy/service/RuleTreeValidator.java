package com.polypilot.strategy.service;

import com.polypilot.strategy.BooleanNode;
import com.polypilot.strategy.CompareNode;
import com.polypilot.strategy.ComparisonOperand;
import com.polypilot.strategy.IndicatorOperand;
import com.polypilot.strategy.LiteralOperand;
import com.polypilot.strategy.MarketFieldCompareNode;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.UnaryBooleanNode;
import com.polypilot.strategy.enums.MarketField;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Validates a whole rule tree for {@code POST /strategies}: a node-count sanity cap, plus every
 * leaf delegated to {@link MarketFieldConditionValidator} or {@link IndicatorConditionValidator}.
 * {@code BOOLEAN}/{@code UNARY_BOOLEAN} arity needs no check here — a malformed one (missing
 * {@code left}/{@code right}/{@code child}) is already structurally impossible by the time a tree
 * reaches this validator, since {@code StrategyNodeDeserializer} rejects it during JSON binding.
 *
 * <p>An external {@code instanceof} walker rather than a method on {@link StrategyNode} itself
 * (unlike {@code evaluate}/{@code collectIndicatorRequests}/{@code usesMarketFields}) — those
 * catalog-aware checks don't belong on the plain domain node classes, which know nothing about
 * {@code IndicatorCatalogService}/{@code MarketFieldCatalogService}.
 */
@Service
@RequiredArgsConstructor
public class RuleTreeValidator {

    /** Sanity cap on rule-tree size — no hard limit specified by the mock, this is a generous guess. */
    private static final int MAX_NODES = 64;

    private final MarketFieldConditionValidator marketFieldConditionValidator;
    private final IndicatorConditionValidator indicatorConditionValidator;

    public void validate(StrategyNode root) {
        validate(root, new AtomicInteger(0));
    }

    private void validate(StrategyNode node, AtomicInteger nodeCount) {
        if (nodeCount.incrementAndGet() > MAX_NODES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Rule tree exceeds the maximum of %d nodes".formatted(MAX_NODES));
        }

        if (node instanceof BooleanNode booleanNode) {
            validate(booleanNode.getLeft(), nodeCount);
            validate(booleanNode.getRight(), nodeCount);
        } else if (node instanceof UnaryBooleanNode unaryBooleanNode) {
            validate(unaryBooleanNode.getChild(), nodeCount);
        } else if (node instanceof CompareNode compareNode) {
            validateOperand(compareNode.getLeft());
            validateOperand(compareNode.getRight());
        } else if (node instanceof MarketFieldCompareNode marketFieldCompareNode) {
            marketFieldConditionValidator.validate(
                    marketFieldCompareNode.getField(),
                    marketFieldCompareNode.getOperator(),
                    marketFieldCompareNode.getField() == MarketField.SENTIMENT
                            ? sentimentName(marketFieldCompareNode)
                            : marketFieldCompareNode.getThreshold());
        } else {
            // Only four StrategyNode subtypes exist; a fifth would be a catalog/deserializer
            // drift, not a user input error.
            throw new IllegalStateException("Unknown StrategyNode type: " + node.getClass());
        }
    }

    private String sentimentName(MarketFieldCompareNode node) {
        return node.getSentimentValue() == null ? null : node.getSentimentValue().name();
    }

    /**
     * {@link LiteralOperand} needs no catalog check — only an {@link IndicatorOperand} (either
     * side of a {@link CompareNode}, since indicator-vs-indicator is just two of these) refers to
     * an indicator that must exist in the catalog.
     */
    private void validateOperand(ComparisonOperand operand) {
        if (operand instanceof IndicatorOperand indicatorOperand) {
            indicatorConditionValidator.validate(
                    indicatorOperand.getIndicator().getIndicatorKey(),
                    indicatorOperand.getIndicator().getParams(),
                    indicatorOperand.getOutputField());
        }
    }
}
