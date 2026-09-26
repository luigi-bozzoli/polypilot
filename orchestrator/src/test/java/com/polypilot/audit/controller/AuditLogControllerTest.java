package com.polypilot.audit.controller;

import com.polypilot.audit.dto.view.AuditLogPageView;
import com.polypilot.audit.dto.view.AuditLogView;
import com.polypilot.audit.service.AuditLogQueryService;
import com.polypilot.enums.alert.ActionType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test on the controller directly — no {@code @WebMvcTest} slice, matching
 * {@code StrategyControllerTest}'s convention. Query-param binding (including the
 * {@code Pageable} resolver / {@code spring.data.web.pageable} caps) is Spring MVC's own
 * concern; what's verified here is delegation and response shape.
 */
class AuditLogControllerTest {

    private final AuditLogQueryService auditLogQueryService = Mockito.mock(AuditLogQueryService.class);
    private final AuditLogController controller = new AuditLogController(auditLogQueryService);
    private final UUID userId = UUID.randomUUID();

    @Test
    void search_delegatesAllFiltersAndPageableToTheQueryService() {
        Pageable pageable = PageRequest.of(0, 25);
        OffsetDateTime since = OffsetDateTime.now().minusDays(1);
        UUID strategyId = UUID.randomUUID();
        AuditLogPageView expected = AuditLogPageView.builder().content(java.util.List.of()).build();
        when(auditLogQueryService.search(userId, ActionType.DRY_RUN_ORDER, strategyId, "m-1", since, pageable))
                .thenReturn(expected);

        AuditLogPageView result = controller.search(userId, ActionType.DRY_RUN_ORDER, strategyId, "m-1", since, pageable);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getById_delegatesToQueryService() {
        UUID id = UUID.randomUUID();
        AuditLogView expected = AuditLogView.builder().id(id).build();
        when(auditLogQueryService.getById(id, userId)).thenReturn(expected);

        AuditLogView result = controller.getById(id, userId);

        assertThat(result).isEqualTo(expected);
    }
}
