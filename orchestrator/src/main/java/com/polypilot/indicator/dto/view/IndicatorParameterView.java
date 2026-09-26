package com.polypilot.indicator.dto.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.polypilot.indicator.enums.ParameterDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * One indicator parameter, flattened. Local and universal parameters are emitted
 * with the same field set regardless of where the metadata comes from:
 * <ul>
 *   <li>{@code allowedValues} — present for every ENUM parameter (inherited from
 *       {@code universal_parameters} when {@link #universal} is {@code true},
 *       otherwise taken from the local {@code constraints.values}).</li>
 *   <li>{@code constraints} — present only for INTEGER / NUMBER parameters
 *       ({@code {min,max,step}} / {@code {min,max}}).</li>
 *   <li>{@code default} — always emitted; {@code null} when the parameter has no
 *       default (e.g. {@code timeframe}).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorParameterView {

    private String key;
    private String name;
    private String description;
    private ParameterDataType dataType;
    private boolean required;

    /** May be {@code null}; always serialized. */
    @JsonProperty("default")
    private String defaultValue;

    /** Every ENUM parameter; omitted otherwise. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> allowedValues;

    /** INTEGER / NUMBER parameters only; omitted otherwise. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> constraints;

    /** True when this parameter is backed by a {@code universal_parameters} row. */
    private boolean universal;
}
