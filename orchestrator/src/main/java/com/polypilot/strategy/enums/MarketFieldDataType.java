package com.polypilot.strategy.enums;

/**
 * The value shape of a {@link MarketField} in {@code GET /strategies/condition-fields}'s
 * catalog — mirrors {@code com.polypilot.indicator.enums.ParameterDataType} in spirit, but
 * kept separate since market fields are resolved independently of the indicator catalog.
 */
public enum MarketFieldDataType {
    NUMBER,
    ENUM
}
