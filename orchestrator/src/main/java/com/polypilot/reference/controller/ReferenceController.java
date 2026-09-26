package com.polypilot.reference.controller;

import com.polypilot.reference.dto.view.TimeframeView;
import com.polypilot.reference.service.ReferenceDataService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only reference-data lookups. Public path is {@code /api/reference}; the dashboard's Vite
 * proxy strips the {@code /api} prefix, same as the market/indicator controllers. Behind the
 * default auth filter — any valid token.
 */
@RestController
@RequestMapping("/reference")
@AllArgsConstructor
public class ReferenceController {

    private final ReferenceDataService referenceDataService;

    /** Enabled candle intervals, in display order — feeds the OHLC chart's timeframe selector. */
    @GetMapping("/timeframes")
    public List<TimeframeView> listTimeframes() {
        return referenceDataService.listEnabledTimeframes();
    }
}
