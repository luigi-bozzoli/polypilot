package com.polypilot.strategy.enums;

/**
 * Unary boolean operators for {@link com.polypilot.strategy.UnaryBooleanNode}.
 * Only {@code NOT} exists today; the type leaves room for future unary ops
 * without disturbing {@link BooleanOperator}'s binary set.
 */
public enum UnaryOperator {
    NOT
}
