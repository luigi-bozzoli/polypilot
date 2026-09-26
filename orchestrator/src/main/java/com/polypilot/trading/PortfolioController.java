package com.polypilot.trading;

import com.polypilot.trading.dto.view.PortfolioSummaryView;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only portfolio aggregation. Public path is {@code /api/portfolio}; the dashboard's Vite
 * proxy strips the {@code /api} prefix. Backs the Overview stat row.
 */
@RestController
@RequestMapping("/portfolio")
@AllArgsConstructor
public class PortfolioController {

    private final PortfolioSummaryService portfolioSummaryService;

    @GetMapping("/summary")
    public PortfolioSummaryView getSummary(@AuthenticationPrincipal UUID userId) {
        return portfolioSummaryService.getSummary(userId);
    }
}
