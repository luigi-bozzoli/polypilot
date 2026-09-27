package com.polypilot.strategy.controller;

import com.polypilot.audit.service.AuditLogQueryService;
import com.polypilot.strategy.dto.request.CreateStrategyRequest;
import com.polypilot.strategy.dto.request.SetStrategyEnabledRequest;
import com.polypilot.strategy.dto.request.UpdateStrategyRequest;
import com.polypilot.strategy.dto.view.ConditionFieldsCatalogView;
import com.polypilot.strategy.dto.view.StrategyDecisionView;
import com.polypilot.strategy.dto.view.StrategyOrderView;
import com.polypilot.strategy.dto.view.StrategyView;
import com.polypilot.strategy.service.*;
import com.polypilot.trading.OrderQueryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Strategy endpoints — full CRUD ({@code GET /condition-fields}, {@code GET}/{@code GET /{id}}/
 * {@code POST}/{@code PUT /{id}}/{@code PUT /{id}/enabled}/{@code DELETE /{id}}), scoped by
 * {@code @AuthenticationPrincipal UUID userId}. Public path is {@code /api/strategies}; the
 * dashboard's Vite proxy strips the {@code /api} prefix, same as the market/indicator
 * controllers. Rule-tree wire format: {@link com.polypilot.strategy.json.StrategyNodeSerializer}.
 */
@RestController
@RequestMapping("/strategies")
@AllArgsConstructor
public class StrategyController {

    private final StrategyConditionFieldsService strategyConditionFieldsService;
    private final StrategyService strategyService;
    private final AuditLogQueryService auditLogQueryService;
    private final OrderQueryService orderQueryService;

    @GetMapping("/condition-fields")
    public ConditionFieldsCatalogView getConditionFields() {
        return strategyConditionFieldsService.getConditionFields();
    }

    @GetMapping
    public List<StrategyView> listStrategies(@AuthenticationPrincipal UUID userId) {
        return strategyService.listStrategies(userId);
    }

    @GetMapping("/{id}")
    public StrategyView getStrategy(@PathVariable("id") UUID id, @AuthenticationPrincipal UUID userId) {
        return strategyService.getStrategy(id, userId);
    }


    @PostMapping
    public ResponseEntity<StrategyView> createStrategy(
            @Valid @RequestBody CreateStrategyRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        StrategyView created = strategyService.create(request, userId);
        return ResponseEntity.created(URI.create("/strategies/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public StrategyView updateStrategy(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateStrategyRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        return strategyService.update(id, userId, request);
    }

    /** Enables/disables a strategy — the scheduler is synced to match in the same call. */
    @PutMapping("/{id}/enabled")
    public StrategyView setEnabled(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SetStrategyEnabledRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        return strategyService.setEnabled(id, userId, request.getEnabled());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrategy(@PathVariable("id") UUID id, @AuthenticationPrincipal UUID userId) {
        strategyService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** Strategy-scoped slice of the audit feed, newest first — backs {@code StrategyDecisionsCard}. */
    @GetMapping("/{id}/decisions")
    public List<StrategyDecisionView> getDecisions(
            @PathVariable("id") UUID id,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return auditLogQueryService.getStrategyDecisions(userId, id, limit);
    }

    /**
     * Strategy-scoped recent orders, newest first — backs {@code StrategyRecentOrdersCard}.
     */
    @GetMapping("/{id}/orders")
    public List<StrategyOrderView> getOrders(
            @PathVariable("id") UUID id,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UUID userId) {
        return orderQueryService.getStrategyOrders(userId, id, limit);
    }
}
