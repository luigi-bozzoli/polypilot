package com.polypilot.indicator.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCatalogView {

    /** Enabled indicators only, ascending by {@code display_order}. */
    private List<IndicatorView> indicators;
}
