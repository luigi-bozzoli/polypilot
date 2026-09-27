package com.polypilot.indicator.controller;

import com.polypilot.indicator.dto.view.IndicatorCatalogView;
import com.polypilot.indicator.dto.view.IndicatorView;
import com.polypilot.indicator.service.IndicatorCatalogService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only technical-indicator catalog.
 */
@RestController
@RequestMapping("/indicators")
@AllArgsConstructor
public class IndicatorController {

    private final IndicatorCatalogService indicatorCatalogService;

    /** Full catalog in one call. */
    @GetMapping
    public IndicatorCatalogView getCatalog() {
        return indicatorCatalogService.getCatalog();
    }

    /** One indicator by its {@code key} (e.g. {@code macd}). 404 if unknown or disabled. */
    @GetMapping("/{key}")
    public IndicatorView getIndicator(@PathVariable("key") String key) {
        return indicatorCatalogService.getIndicator(key);
    }
}
