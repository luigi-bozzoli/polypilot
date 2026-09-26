package com.polypilot.trading;

import com.polypilot.trading.dto.view.PositionView;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only positions endpoint. Public path is {@code /api/positions}; the dashboard's Vite proxy
 * strips the {@code /api} prefix, same as the market/strategy controllers. Every row is currently
 * a simulated dry-run position (no {@code POLYPILOT_LIVE_MODE} exists yet) — see {@code
 * OpenTradeService}/{@code docs/open_trades.md}.
 */
@RestController
@RequestMapping("/positions")
@AllArgsConstructor
public class PositionController {

    private final PositionQueryService positionQueryService;

    /** The caller's open positions, newest-opened first — backs the Overview positions widget. */
    @GetMapping
    public List<PositionView> getOpenPositions(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return positionQueryService.getOpenPositions(userId, limit);
    }
}
