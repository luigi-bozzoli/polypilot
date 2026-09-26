package com.polypilot.indicator.dto.view;

import com.polypilot.indicator.enums.IndicatorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Outbound view of one catalog indicator: metadata plus its full ordered
 * parameter list (local and universal already flattened into one uniform shape)
 * and its outputs. Also the body of {@code GET /api/indicators/{key}}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorView {

    private String key;
    private String name;
    private String abbreviation;
    private IndicatorCategory category;
    private String description;

    private List<IndicatorParameterView> parameters;
    private List<IndicatorOutputView> outputs;
}
