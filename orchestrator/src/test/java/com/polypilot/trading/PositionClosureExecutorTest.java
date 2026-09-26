package com.polypilot.trading;

import com.polypilot.entity.AuditLog;
import com.polypilot.entity.Position;
import com.polypilot.enums.alert.ActionType;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.AuditLogRepository;
import com.polypilot.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link PositionClosureExecutor#closePosition}: eligibility checks (position must be
 * OPEN, market must not be OPEN, a settlement price must exist) and the P&L / audit-log write on a
 * successful close.
 */
@ExtendWith(MockitoExtension.class)
class PositionClosureExecutorTest {

    private static final String MARKET_ID = "market-1";

    @Mock
    private PositionRepository positionRepository;
    @Mock
    private MarketRepository marketRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private PositionClosureExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new PositionClosureExecutor(
                positionRepository, marketRepository, auditLogRepository, new ObjectMapper());
    }

    @Test
    void positionNoLongerFound_skips() {
        UUID id = UUID.randomUUID();
        when(positionRepository.findById(id)).thenReturn(Optional.empty());

        boolean closed = executor.closePosition(id);

        assertThat(closed).isFalse();
        verify(marketRepository, never()).findById(any());
    }

    @Test
    void positionAlreadyClosed_skips() {
        Position position = position(PositionStatus.CLOSED, MarketOutcome.YES, "10", "0.5");
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));

        boolean closed = executor.closePosition(position.getId());

        assertThat(closed).isFalse();
        verify(marketRepository, never()).findById(any());
    }

    @Test
    void marketNoLongerExists_skips() {
        Position position = position(PositionStatus.OPEN, MarketOutcome.YES, "10", "0.5");
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.empty());

        boolean closed = executor.closePosition(position.getId());

        assertThat(closed).isFalse();
        verify(positionRepository, never()).save(any());
    }

    @Test
    void marketStillOpen_skips() {
        Position position = position(PositionStatus.OPEN, MarketOutcome.YES, "10", "0.5");
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.7"), new BigDecimal("0.3"))));

        boolean closed = executor.closePosition(position.getId());

        assertThat(closed).isFalse();
        verify(positionRepository, never()).save(any());
    }

    @Test
    void marketClosedButNoPriceForTokenSide_skipsAndLeavesPositionOpen() {
        Position position = position(PositionStatus.OPEN, MarketOutcome.NO, "10", "0.5");
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.CLOSED, new BigDecimal("0.7"), null)));

        boolean closed = executor.closePosition(position.getId());

        assertThat(closed).isFalse();
        assertThat(position.getStatus()).isEqualTo(PositionStatus.OPEN);
        verify(positionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void marketClosed_closesPositionWithRealizedPnlAndWritesAuditLog() {
        // entry 0.5, settlement 0.7, size 10 -> pnl = (0.7 - 0.5) * 10 = 2.0
        Position position = position(PositionStatus.OPEN, MarketOutcome.YES, "10", "0.5");
        position.setRealizedPnl(new BigDecimal("1.0000"));
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.CLOSED, new BigDecimal("0.7"), new BigDecimal("0.3"))));

        boolean closed = executor.closePosition(position.getId());

        assertThat(closed).isTrue();
        assertThat(position.getStatus()).isEqualTo(PositionStatus.CLOSED);
        assertThat(position.getCurrentPrice()).isEqualByComparingTo("0.7");
        assertThat(position.getUnrealizedPnl()).isEqualByComparingTo("0");
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("3.0000"); // 1.0 existing + 2.0 new
        assertThat(position.getClosedAt()).isNotNull();
        verify(positionRepository).save(position);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAction()).isEqualTo(ActionType.POSITION_CLOSED);
        assertThat(auditLog.getMarketId()).isEqualTo(MARKET_ID);
        assertThat(auditLog.getUserId()).isEqualTo(position.getUserId());
        assertThat(auditLog.getIsDryRun()).isEqualTo(position.getIsDryRun());
        assertThat(auditLog.getSignals()).contains("\"settlementPrice\":0.7");
    }

    @Test
    void resolvedMarket_alsoClosesPosition() {
        Position position = position(PositionStatus.OPEN, MarketOutcome.NO, "5", "0.2");
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.RESOLVED, new BigDecimal("0"), new BigDecimal("1"))));

        boolean closed = executor.closePosition(position.getId());

        assertThat(closed).isTrue();
        assertThat(position.getStatus()).isEqualTo(PositionStatus.CLOSED);
        // entry 0.2, settlement 1 (NO side), size 5 -> pnl = (1 - 0.2) * 5 = 4.0
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("4.0000");
    }

    private static Position position(PositionStatus status, MarketOutcome tokenSide, String size, String avgEntryPrice) {
        return Position.builder()
                .id(UUID.randomUUID())
                .marketId(MARKET_ID)
                .userId(UUID.randomUUID())
                .tokenSide(tokenSide)
                .size(new BigDecimal(size))
                .avgEntryPrice(new BigDecimal(avgEntryPrice))
                .currentPrice(new BigDecimal(avgEntryPrice))
                .unrealizedPnl(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .status(status)
                .isDryRun(true)
                .openedAt(OffsetDateTime.now().minusDays(1))
                .build();
    }

    private static Market market(MarketStatus status, BigDecimal upPrice, BigDecimal downPrice) {
        return Market.builder()
                .id(MARKET_ID)
                .status(status)
                .upPrice(upPrice)
                .downPrice(downPrice)
                .build();
    }
}
