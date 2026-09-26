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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock StrategyRepository strategyRepository;
    @Mock MarketRepository marketRepository;

    // Real mapper — it is pure translation, nothing worth stubbing.
    private AuditLogQueryService service() {
        return new AuditLogQueryService(auditLogRepository, strategyRepository, marketRepository,
                new AuditLogViewMapper());
    }

    private final UUID userId = UUID.randomUUID();

    private static AuditLog auditLog(UUID id, UUID strategyId, String marketId, ActionType action) {
        return AuditLog.builder()
                .id(id)
                .strategyId(strategyId)
                .marketId(marketId)
                .userId(UUID.randomUUID())
                .action(action)
                .isDryRun(true)
                .reasoning("4 / 4 rules passed")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void search_mapsPageAndResolvesStrategyAndMarketNamesInBatch() {
        UUID strategyId = UUID.randomUUID();
        String marketId = "m-1";
        AuditLog row = auditLog(UUID.randomUUID(), strategyId, marketId, ActionType.STRATEGY_EVALUATED);
        Pageable pageable = PageRequest.of(0, 25);
        Page<AuditLog> page = new PageImpl<>(List.of(row), pageable, 1);

        when(auditLogRepository.search(eq(userId), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);
        when(strategyRepository.findAllById(List.of(strategyId)))
                .thenReturn(List.of(Strategy.builder().id(strategyId).name("BTC Bullish Momentum").build()));
        when(marketRepository.findAllById(List.of(marketId)))
                .thenReturn(List.of(Market.builder().id(marketId).question("Will BTC exceed $120k?").build()));

        AuditLogPageView result = service().search(userId, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        AuditLogView view = result.getContent().get(0);
        assertThat(view.getStrategyName()).isEqualTo("BTC Bullish Momentum");
        assertThat(view.getMarketQuestion()).isEqualTo("Will BTC exceed $120k?");
    }

    @Test
    void search_withNoStrategyOrMarketOnRows_skipsBatchLookupsAndLeavesNamesNull() {
        AuditLog row = auditLog(UUID.randomUUID(), null, null, ActionType.MARKET_RESOLVED);
        Pageable pageable = PageRequest.of(0, 25);
        Page<AuditLog> page = new PageImpl<>(List.of(row), pageable, 1);

        when(auditLogRepository.search(any(), any(), any(), any(), any(), any())).thenReturn(page);

        AuditLogPageView result = service().search(userId, ActionType.MARKET_RESOLVED, null, null, null, pageable);

        assertThat(result.getContent().get(0).getStrategyName()).isNull();
        assertThat(result.getContent().get(0).getMarketQuestion()).isNull();
        org.mockito.Mockito.verifyNoInteractions(strategyRepository);
        org.mockito.Mockito.verifyNoInteractions(marketRepository);
    }

    @Test
    void getById_unknownOrNotOwned_throws404() {
        UUID id = UUID.randomUUID();
        when(auditLogRepository.findByIdAndUserId(id, userId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service().getById(id, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_strategyOrMarketNoLongerExists_nameIsNullNotAnError() {
        UUID id = UUID.randomUUID();
        UUID strategyId = UUID.randomUUID();
        AuditLog row = auditLog(id, strategyId, "m-gone", ActionType.STRATEGY_EVALUATED);
        when(auditLogRepository.findByIdAndUserId(id, userId)).thenReturn(java.util.Optional.of(row));
        when(strategyRepository.findAllById(List.of(strategyId))).thenReturn(List.of());
        when(marketRepository.findAllById(List.of("m-gone"))).thenReturn(List.of());

        AuditLogView view = service().getById(id, userId);

        assertThat(view.getStrategyName()).isNull();
        assertThat(view.getMarketQuestion()).isNull();
        assertThat(view.getId()).isEqualTo(id);
    }

    @Test
    void getMarketDecisions_mapsAuditRowsToEngineDecisionShape() {
        UUID strategyId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        AuditLog row = auditLog(auditId, strategyId, "m-1", ActionType.DRY_RUN_ORDER);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(auditLogRepository.search(eq(userId), isNull(), isNull(), eq("m-1"), isNull(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(strategyRepository.findAllById(List.of(strategyId)))
                .thenReturn(List.of(Strategy.builder().id(strategyId).name("BTC Bullish Momentum").build()));

        MarketDecisionsView result = service().getMarketDecisions(userId, "m-1", 20);

        assertThat(result.getMarketId()).isEqualTo("m-1");
        assertThat(result.getDecisions()).hasSize(1);
        EngineDecisionView decision = result.getDecisions().get(0);
        assertThat(decision.getKind()).isEqualTo("DRY_RUN_ORDER");
        assertThat(decision.getAuditLogId()).isEqualTo(auditId);
        assertThat(decision.getId()).isEqualTo(auditId);
        assertThat(decision.getStrategyName()).isEqualTo("BTC Bullish Momentum");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getStrategyDecisions_mapsAuditRowsToStrategyDecisionShape() {
        UUID strategyId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        AuditLog row = auditLog(auditId, strategyId, null, ActionType.ORDER_SKIPPED);

        when(auditLogRepository.search(eq(userId), isNull(), eq(strategyId), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(strategyRepository.findAllById(List.of(strategyId)))
                .thenReturn(List.of(Strategy.builder().id(strategyId).name("ETH Hourly Mean-Reversion").build()));

        List<StrategyDecisionView> result = service().getStrategyDecisions(userId, strategyId, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKind()).isEqualTo("ORDER_SKIPPED");
        assertThat(result.get(0).getAuditLogId()).isEqualTo(auditId);
        assertThat(result.get(0).getDetail()).isEqualTo("4 / 4 rules passed");
    }
}
