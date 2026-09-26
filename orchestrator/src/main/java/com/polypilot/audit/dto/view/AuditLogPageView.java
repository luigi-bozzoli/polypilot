package com.polypilot.audit.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Thin, stable wrapper around a Spring Data {@code Page<AuditLogView>} — body of
 * {@code GET /audit-logs}. Wrapping rather than serializing {@code Page} directly
 * keeps the response shape independent of the pagination implementation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPageView {

    private List<AuditLogView> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
