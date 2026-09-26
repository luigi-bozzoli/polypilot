package com.polypilot.indicator.enums;

/**
 * The value shape of an indicator parameter, which also dictates the shape of
 * its {@code constraints} JSONB payload:
 * <ul>
 *   <li>{@code INTEGER} &rarr; {@code {"min":…,"max":…,"step":…}}</li>
 *   <li>{@code NUMBER}  &rarr; {@code {"min":…,"max":…}}</li>
 *   <li>{@code ENUM}    &rarr; {@code {"values":[…]}} for a local list, or empty
 *       when {@code universal_key} is set and the list is inherited from
 *       {@code universal_parameters.allowed_values}</li>
 * </ul>
 * Mirrors the {@code data_type} CHECK constraints in {@code 001_schema.sql}.
 */
public enum ParameterDataType {
    INTEGER,
    NUMBER,
    ENUM
}
