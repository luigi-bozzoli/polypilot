package com.polypilot.indicator.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full technical-indicator catalog in one payload — the response of
 * {@code GET /api/indicators}. Contract: {@code contracts/indicator-catalog.md}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCatalogView {

    /** Enabled indicators only, ascending by {@code display_order}. */
    private List<IndicatorView> indicators;
}
