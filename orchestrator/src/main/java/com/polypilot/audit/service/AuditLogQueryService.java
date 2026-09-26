package com.polypilot.audit.service;

import com.polypilot.audit.dto.view.AuditLogPageView;
import com.polypilot.audit.dto.view.AuditLogView;
import com.polypilot.audit.mapper.AuditLogViewMapper;
import com.polypilot.entity.AuditLog;
import com.polypilot.entity.Strategy;
import com.polypilot.enums.alert.ActionType;
import com.polypilot.market.dto.view.EngineDecisionView;
import com.polypilot.market.dto.view.MarketDecisionsView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.AuditLogRepository;
import com.polypilot.strategy.dto.view.StrategyDecisionView;
import com.polypilot.strategy.repository.StrategyRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-side queries backing the dashboard's audit feed — the general
 * {@code GET /audit-logs} list/detail, and the market-/strategy-scoped slices
 * consumed by {@code EngineDecisionsCard}/{@code StrategyDecisionsCard}. Pure
 * fan-in over existing repositories — no writes.
 */
@Service
@AllArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final StrategyRepository strategyRepository;
    private final MarketRepository marketRepository;
    private final AuditLogViewMapper auditLogViewMapper;

    @Transactional(readOnly = true)
    public AuditLogPageView search(UUID userId, ActionType action, UUID strategyId, String marketId,
                                    OffsetDateTime since, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.search(userId, action, strategyId, marketId, since, pageable);
        List<AuditLogView> content = toViews(page.getContent());

        return AuditLogPageView.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogView getById(UUID id, UUID userId) {
        AuditLog auditLog = auditLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Audit log not found: " + id));

        return auditLogViewMapper.toView(auditLog, strategyNamesById(List.of(auditLog)),
                marketQuestionsById(List.of(auditLog)));
    }

    /**
     * Market-scoped slice, newest first — backs {@code EngineDecisionsCard} /
     * {@code contracts/market-engine-decisions.md}.
     */
    @Transactional(readOnly = true)
    public MarketDecisionsView getMarketDecisions(UUID userId, String marketId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLogView> views = toViews(
                auditLogRepository.search(userId, null, null, marketId, null, pageable).getContent());

        List<EngineDecisionView> decisions = views.stream()
                .map(v -> EngineDecisionView.builder()
                        .id(v.getId())
                        .at(v.getCreatedAt())
                        .kind(v.getAction().name())
                        .detail(v.getReasoning())
                        .strategyId(v.getStrategyId())
                        .strategyName(v.getStrategyName())
                        .auditLogId(v.getId())
                        .build())
                .toList();

        return MarketDecisionsView.builder().marketId(marketId).decisions(decisions).build();
    }

    /** Strategy-scoped slice, newest first — backs {@code StrategyDecisionsCard}. */
    @Transactional(readOnly = true)
    public List<StrategyDecisionView> getStrategyDecisions(UUID userId, UUID strategyId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLogView> views = toViews(
                auditLogRepository.search(userId, null, strategyId, null, null, pageable).getContent());

        return views.stream()
                .map(v -> StrategyDecisionView.builder()
                        .occurredAt(v.getCreatedAt())
                        .kind(v.getAction().name())
                        .detail(v.getReasoning())
                        .auditLogId(v.getId())
                        .build())
                .toList();
    }

    private List<AuditLogView> toViews(List<AuditLog> auditLogs) {
        Map<UUID, String> strategyNames = strategyNamesById(auditLogs);
        Map<String, String> marketQuestions = marketQuestionsById(auditLogs);

        return auditLogs.stream()
                .map(auditLog -> auditLogViewMapper.toView(auditLog, strategyNames, marketQuestions))
                .toList();
    }

    /** Batch lookup — one query for every distinct {@code strategyId} on the page, avoiding N+1. */
    private Map<UUID, String> strategyNamesById(List<AuditLog> auditLogs) {
        List<UUID> strategyIds = auditLogs.stream()
                .map(AuditLog::getStrategyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (strategyIds.isEmpty()) {
            return Map.of();
        }

        return strategyRepository.findAllById(strategyIds).stream()
                .collect(Collectors.toMap(Strategy::getId, Strategy::getName));
    }

    /** Batch lookup — one query for every distinct {@code marketId} on the page, avoiding N+1. */
    private Map<String, String> marketQuestionsById(List<AuditLog> auditLogs) {
        List<String> marketIds = auditLogs.stream()
                .map(AuditLog::getMarketId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (marketIds.isEmpty()) {
            return Map.of();
        }

        return marketRepository.findAllById(marketIds).stream()
                .collect(Collectors.toMap(Market::getId, Market::getQuestion));
    }
}
