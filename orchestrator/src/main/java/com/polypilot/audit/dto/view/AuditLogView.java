package com.polypilot.audit.dto.view;

import com.polypilot.enums.alert.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound view of one {@link com.polypilot.entity.AuditLog} row, widened with the
 * owning strategy's name and the market's question — both resolved by batch lookup
 * in {@link com.polypilot.audit.service.AuditLogQueryService} rather than a JPA
 * relation, since {@code AuditLog} only stores raw FK columns. {@code strategyName}/
 * {@code marketQuestion} are {@code null} when the row has no strategy/market, or
 * the referenced row no longer exists. {@code signals} is the raw {@code jsonb}
 * string, passed through as-is.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogView {

    private UUID id;
    private OffsetDateTime createdAt;
    private ActionType action;
    private Boolean isDryRun;
    private UUID strategyId;
    private String strategyName;
    private String marketId;
    private String marketQuestion;
    private UUID orderId;
    private String reasoning;
    private String signals;
}
