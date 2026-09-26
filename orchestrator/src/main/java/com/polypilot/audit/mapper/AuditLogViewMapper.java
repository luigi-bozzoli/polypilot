package com.polypilot.audit.mapper;

import com.polypilot.audit.dto.view.AuditLogView;
import com.polypilot.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Entity → outbound view mapping for the audit read endpoints. Hand-written, same
 * convention as {@link com.polypilot.market.mapper.MarketViewMapper} — strategy
 * name / market question are supplied as pre-fetched maps (batch-looked-up by the
 * caller) rather than traversed via a JPA relation, since {@link AuditLog} has none.
 */
@Component
public class AuditLogViewMapper {

    public AuditLogView toView(AuditLog auditLog, Map<UUID, String> strategyNamesById,
                                Map<String, String> marketQuestionsById) {
        return AuditLogView.builder()
                .id(auditLog.getId())
                .createdAt(auditLog.getCreatedAt())
                .action(auditLog.getAction())
                .isDryRun(auditLog.getIsDryRun())
                .strategyId(auditLog.getStrategyId())
                .strategyName(auditLog.getStrategyId() != null
                        ? strategyNamesById.get(auditLog.getStrategyId()) : null)
                .marketId(auditLog.getMarketId())
                .marketQuestion(auditLog.getMarketId() != null
                        ? marketQuestionsById.get(auditLog.getMarketId()) : null)
                .orderId(auditLog.getOrderId())
                .reasoning(auditLog.getReasoning())
                .signals(auditLog.getSignals())
                .build();
    }
}
