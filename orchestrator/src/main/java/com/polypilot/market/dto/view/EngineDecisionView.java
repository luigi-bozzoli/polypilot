package com.polypilot.market.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row of the market-scoped engine-decisions feed. Shape matches
 * {@code EngineDecision} in {@code contracts/market-engine-decisions.md} /
 * {@code dashboard/src/features/market/types.ts} — {@code id} and
 * {@code auditLogId} are the same underlying {@code audit_logs} row id; the
 * contract keeps them as separate fields, so both are populated identically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineDecisionView {

    private UUID id;
    private OffsetDateTime at;
    private String kind;
    private String detail;
    private UUID strategyId;
    private String strategyName;
    private UUID auditLogId;
}
