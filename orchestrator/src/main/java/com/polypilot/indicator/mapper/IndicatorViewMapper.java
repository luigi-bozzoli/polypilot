package com.polypilot.indicator.mapper;

import com.polypilot.indicator.dto.view.IndicatorOutputView;
import com.polypilot.indicator.dto.view.IndicatorParameterView;
import com.polypilot.indicator.dto.view.IndicatorView;
import com.polypilot.indicator.entity.Indicator;
import com.polypilot.indicator.entity.IndicatorOutput;
import com.polypilot.indicator.entity.IndicatorParameter;
import com.polypilot.indicator.entity.UniversalParameter;
import com.polypilot.indicator.enums.ParameterDataType;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;

/**
 * Entity &rarr; outbound view mapping for the indicator catalog. The
 * indicator/output mappings are generated; {@link #toParameterView} is
 * hand-written because it resolves the local-vs-universal split at map time so
 * every parameter flattens to one uniform shape (see {@link IndicatorParameterView}).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IndicatorViewMapper {

    List<IndicatorView> toViews(List<Indicator> indicators);

    IndicatorView toView(Indicator indicator);

    IndicatorOutputView toOutputView(IndicatorOutput output);

    /**
     * Flatten one parameter. When it is backed by a {@link UniversalParameter}
     * the allowed-value list and (fallback) default come from there; otherwise
     * they come from the local {@code constraints} JSONB. {@code constraints} is
     * only surfaced for numeric parameters.
     */
    default IndicatorParameterView toParameterView(IndicatorParameter p) {
        if (p == null) {
            return null;
        }

        UniversalParameter universal = p.getUniversal();
        boolean isUniversal = universal != null;

        List<String> allowedValues = null;
        Map<String, Object> constraints = null;

        if (p.getDataType() == ParameterDataType.ENUM) {
            if (isUniversal) {
                allowedValues = universal.getAllowedValues();
            } else if (p.getConstraints() != null
                    && p.getConstraints().get("values") instanceof List<?> values) {
                allowedValues = values.stream().map(String::valueOf).toList();
            }
        } else {
            Map<String, Object> local = p.getConstraints();
            if (local != null && !local.isEmpty()) {
                constraints = local;
            }
        }

        String defaultValue = p.getDefaultValue();
        if (defaultValue == null && isUniversal) {
            defaultValue = universal.getDefaultValue();
        }

        return IndicatorParameterView.builder()
                .key(p.getKey())
                .name(p.getName())
                .description(p.getDescription())
                .dataType(p.getDataType())
                .required(Boolean.TRUE.equals(p.getRequired()))
                .defaultValue(defaultValue)
                .allowedValues(allowedValues)
                .constraints(constraints)
                .universal(isUniversal)
                .build();
    }
}
