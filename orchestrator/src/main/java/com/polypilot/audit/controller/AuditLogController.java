package com.polypilot.audit.controller;

import com.polypilot.audit.dto.view.AuditLogPageView;
import com.polypilot.audit.dto.view.AuditLogView;
import com.polypilot.audit.service.AuditLogQueryService;
import com.polypilot.enums.alert.ActionType;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only audit feed. Public path is {@code /api/audit-logs}; the dashboard's Vite proxy
 * strips the {@code /api} prefix, same as the market/strategy/indicator controllers. Behind
 * the default auth filter — any valid token, scoped to the caller's own rows.
 *
 * <p>Backs the full audit-log list/detail mocks ({@code 19-audit-log.html},
 * {@code 06-audit-log-detail.html}); the market-scoped
 * and strategy-scoped ({@code StrategyDecisionsCard.tsx}) feeds are thinner slices of this same
 * query, added on {@code MarketController}/{@code StrategyController} respectively.
 *
 * <p>Only what {@code audit_logs} actually persists is returned — no rule-by-rule breakdown or
 * signal-bar data, since {@code StrategyEvaluationService.evaluate()} doesn't capture that today).
 */
@RestController
@RequestMapping("/audit-logs")
@AllArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    public AuditLogPageView search(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) ActionType action,
            @RequestParam(required = false) UUID strategyId,
            @RequestParam(required = false) String marketId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditLogQueryService.search(userId, action, strategyId, marketId, since, pageable);
    }

    @GetMapping("/{id}")
    public AuditLogView getById(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return auditLogQueryService.getById(id, userId);
    }
}
