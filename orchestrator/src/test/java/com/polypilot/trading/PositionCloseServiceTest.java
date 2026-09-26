package com.polypilot.trading;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link PositionCloseService#closeEligiblePositions()}: the batch loop delegates one
 * call per OPEN position to {@link PositionClosureExecutor}, and one bad position (an exception)
 * never stops the rest of the batch — same isolation pattern {@code MarketSyncServiceTest} would
 * exercise for {@code MarketItemSyncService}.
 */
@ExtendWith(MockitoExtension.class)
class PositionCloseServiceTest {

    @Mock
    private PositionRepository positionRepository;
    @Mock
    private PositionClosureExecutor positionClosureExecutor;

    private PositionCloseService service;

    @BeforeEach
    void setUp() {
        service = new PositionCloseService(positionRepository, positionClosureExecutor);
    }

    @Test
    void noOpenPositions_touchesTheExecutorNever() {
        when(positionRepository.findAllByStatus(PositionStatus.OPEN)).thenReturn(List.of());

        service.closeEligiblePositions();

        verify(positionClosureExecutor, times(0)).closePosition(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closesEachOpenPositionOnce() {
        Position a = position();
        Position b = position();
        when(positionRepository.findAllByStatus(PositionStatus.OPEN)).thenReturn(List.of(a, b));
        when(positionClosureExecutor.closePosition(a.getId())).thenReturn(true);
        when(positionClosureExecutor.closePosition(b.getId())).thenReturn(false);

        service.closeEligiblePositions();

        verify(positionClosureExecutor).closePosition(a.getId());
        verify(positionClosureExecutor).closePosition(b.getId());
    }

    @Test
    void onePositionThrows_theRestOfTheBatchStillRuns() {
        Position failing = position();
        Position ok = position();
        when(positionRepository.findAllByStatus(PositionStatus.OPEN)).thenReturn(List.of(failing, ok));
        when(positionClosureExecutor.closePosition(failing.getId()))
                .thenThrow(new RuntimeException("boom"));
        when(positionClosureExecutor.closePosition(ok.getId())).thenReturn(true);

        service.closeEligiblePositions();

        verify(positionClosureExecutor).closePosition(eq(ok.getId()));
    }

    private static Position position() {
        return Position.builder()
                .id(UUID.randomUUID())
                .marketId("market-1")
                .userId(UUID.randomUUID())
                .tokenSide(MarketOutcome.YES)
                .size(BigDecimal.TEN)
                .avgEntryPrice(new BigDecimal("0.5"))
                .status(PositionStatus.OPEN)
                .isDryRun(true)
                .build();
    }
}
