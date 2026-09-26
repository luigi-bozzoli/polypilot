package com.polypilot.trading;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.repository.OrderRepository;
import com.polypilot.repository.PositionRepository;
import com.polypilot.trading.dto.view.PortfolioSummaryView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test on the service directly (no Spring context) — see {@code OrderQueryServiceTest} for
 * the convention this mirrors.
 */
class PortfolioSummaryServiceTest {

    private final PositionRepository positionRepository = Mockito.mock(PositionRepository.class);
    private final OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
    private final PortfolioSummaryService service = new PortfolioSummaryService(positionRepository, orderRepository);

    private final UUID userId = UUID.randomUUID();

    @Test
    void getSummary_sumsUnrealizedAndRealizedPnlAcrossPositions() {
        Position open1 = openPosition(BigDecimal.valueOf(41.20));
        Position open2 = openPosition(BigDecimal.valueOf(-12.00));
        Position closed = closedPosition(BigDecimal.valueOf(88.00));

        when(positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN))
                .thenReturn(List.of(open1, open2));
        when(positionRepository.findAllByUserIdAndStatusAndClosedAtAfter(eq(userId), eq(PositionStatus.CLOSED), any()))
                .thenReturn(List.of(closed));
        when(orderRepository.countByUserIdAndPlacedAtAfter(eq(userId), any())).thenReturn(31L);

        PortfolioSummaryView result = service.getSummary(userId);

        assertThat(result.getUnrealizedPnl()).isEqualByComparingTo(BigDecimal.valueOf(29.20));
        assertThat(result.getRealizedPnl7d()).isEqualByComparingTo(BigDecimal.valueOf(88.00));
        assertThat(result.getDryOrdersToday()).isEqualTo(31L);
        assertThat(result.getOpenPositionCount()).isEqualTo(2);
    }

    @Test
    void getSummary_returnsZeroesWhenThereIsNoPortfolioActivity() {
        when(positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN))
                .thenReturn(List.of());
        when(positionRepository.findAllByUserIdAndStatusAndClosedAtAfter(eq(userId), eq(PositionStatus.CLOSED), any()))
                .thenReturn(List.of());
        when(orderRepository.countByUserIdAndPlacedAtAfter(eq(userId), any())).thenReturn(0L);

        PortfolioSummaryView result = service.getSummary(userId);

        assertThat(result.getUnrealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getRealizedPnl7d()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getDryOrdersToday()).isZero();
        assertThat(result.getOpenPositionCount()).isZero();
    }

    private Position openPosition(BigDecimal unrealizedPnl) {
        return Position.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(PositionStatus.OPEN)
                .unrealizedPnl(unrealizedPnl)
                .realizedPnl(BigDecimal.ZERO)
                .build();
    }

    private Position closedPosition(BigDecimal realizedPnl) {
        return Position.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(PositionStatus.CLOSED)
                .realizedPnl(realizedPnl)
                .closedAt(OffsetDateTime.now())
                .build();
    }
}
