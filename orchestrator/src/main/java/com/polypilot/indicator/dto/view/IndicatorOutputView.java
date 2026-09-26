package com.polypilot.indicator.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.polypilot.indicator.enums.ValueScale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One value an indicator produces. {@code default} marks the output preselected
 * in the builder's output picker (and the only one for single-output indicators).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorOutputView {

    private String key;
    private String name;
    private ValueScale valueScale;

    @JsonProperty("default")
    private Boolean isDefault;
}
