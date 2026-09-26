package com.polypilot.trading;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.repository.OrderRepository;
import com.polypilot.repository.PositionRepository;
import com.polypilot.trading.dto.view.PortfolioSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Read-side aggregation backing the Overview stat row. Pure fan-in over {@link
 * PositionRepository}/{@link OrderRepository} — no "equity" figure, since nothing in the schema
 * stores a starting-capital baseline (see {@code docs/polypilot-todo.md}); only what {@code
 * positions}/{@code orders} can actually support is exposed.
 */
@Service
@RequiredArgsConstructor
public class PortfolioSummaryService {

    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PortfolioSummaryView getSummary(UUID userId) {
        List<Position> openPositions = positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN);
        List<Position> recentlyClosed = positionRepository.findAllByUserIdAndStatusAndClosedAtAfter(
                userId, PositionStatus.CLOSED, OffsetDateTime.now().minusDays(7));

        BigDecimal unrealizedPnl = sum(openPositions, Position::getUnrealizedPnl);
        BigDecimal realizedPnl7d = sum(recentlyClosed, Position::getRealizedPnl);

        OffsetDateTime todayStartUtc = LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
        long dryOrdersToday = orderRepository.countByUserIdAndPlacedAtAfter(userId, todayStartUtc);

        return PortfolioSummaryView.builder()
                .unrealizedPnl(unrealizedPnl)
                .realizedPnl7d(realizedPnl7d)
                .dryOrdersToday(dryOrdersToday)
                .openPositionCount(openPositions.size())
                .build();
    }

    private BigDecimal sum(List<Position> positions, Function<Position, BigDecimal> field) {
        return positions.stream()
                .map(field)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
