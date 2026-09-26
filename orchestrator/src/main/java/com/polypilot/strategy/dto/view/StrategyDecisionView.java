package com.polypilot.strategy.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row of the strategy-scoped decisions feed, body of
 * {@code GET /strategies/{id}/decisions}. Shape matches the proposal in
 * {@code StrategyDecisionsCard.tsx}'s doc comment: {@code occurredAt}, {@code kind}
 * (the {@code audit_logs.action} value), {@code detail}, {@code auditLogId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyDecisionView {

    private OffsetDateTime occurredAt;
    private String kind;
    private String detail;
    private UUID auditLogId;
}
