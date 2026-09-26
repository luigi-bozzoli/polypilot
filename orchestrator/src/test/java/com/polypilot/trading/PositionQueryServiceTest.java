package com.polypilot.trading;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.PositionRepository;
import com.polypilot.trading.dto.view.PositionView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test on the service directly (no Spring context) — see {@code OrderQueryServiceTest} /
 * {@code orchestrator/CLAUDE.md}'s Tests section for the convention this mirrors.
 */
class PositionQueryServiceTest {

    private final PositionRepository positionRepository = Mockito.mock(PositionRepository.class);
    private final MarketRepository marketRepository = Mockito.mock(MarketRepository.class);
    private final PositionQueryService service = new PositionQueryService(positionRepository, marketRepository);

    private final UUID userId = UUID.randomUUID();

    @Test
    void getOpenPositions_mapsAnOpenPositionAndResolvesTheMarketQuestion() {
        Position position = position("0x4f2a9c", BigDecimal.valueOf(120), BigDecimal.valueOf(0.63));
        when(positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN))
                .thenReturn(List.of(position));
        when(marketRepository.findAllById(List.of("0x4f2a9c")))
                .thenReturn(List.of(market("0x4f2a9c", "BTC Up 8PM ET")));

        List<PositionView> result = service.getOpenPositions(userId, 20);

        assertThat(result).hasSize(1);
        PositionView view = result.get(0);
        assertThat(view.getMarketId()).isEqualTo("0x4f2a9c");
        assertThat(view.getMarketQuestion()).isEqualTo("BTC Up 8PM ET");
        assertThat(view.getTokenSide()).isEqualTo(MarketOutcome.YES);
        assertThat(view.getSize()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(view.getAvgEntryPrice()).isEqualByComparingTo(BigDecimal.valueOf(0.63));
    }

    @Test
    void getOpenPositions_fallsBackToTheRawMarketIdWhenTheMarketCannotBeResolved() {
        Position position = position("0xunknown", BigDecimal.ONE, BigDecimal.valueOf(0.5));
        when(positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN))
                .thenReturn(List.of(position));
        when(marketRepository.findAllById(List.of("0xunknown"))).thenReturn(List.of());

        List<PositionView> result = service.getOpenPositions(userId, 20);

        assertThat(result.get(0).getMarketQuestion()).isEqualTo("0xunknown");
    }

    @Test
    void getOpenPositions_returnsEmptyListWhenTheCallerHasNoOpenPositions() {
        when(positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN))
                .thenReturn(List.of());

        List<PositionView> result = service.getOpenPositions(userId, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void getOpenPositions_capsTheResultAtTheGivenLimit() {
        Position first = position("0x1", BigDecimal.ONE, BigDecimal.valueOf(0.5));
        Position second = position("0x2", BigDecimal.ONE, BigDecimal.valueOf(0.5));
        when(positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN))
                .thenReturn(List.of(first, second));
        when(marketRepository.findAllById(List.of("0x1", "0x2"))).thenReturn(List.of());

        List<PositionView> result = service.getOpenPositions(userId, 1);

        assertThat(result).hasSize(1);
    }

    private Position position(String marketId, BigDecimal size, BigDecimal avgEntryPrice) {
        OffsetDateTime now = OffsetDateTime.now();
        return Position.builder()
                .id(UUID.randomUUID())
                .marketId(marketId)
                .userId(userId)
                .tokenSide(MarketOutcome.YES)
                .size(size)
                .avgEntryPrice(avgEntryPrice)
                .currentPrice(avgEntryPrice)
                .unrealizedPnl(BigDecimal.ZERO)
                .status(PositionStatus.OPEN)
                .isDryRun(true)
                .openedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Market market(String id, String question) {
        Market market = new Market();
        market.setId(id);
        market.setQuestion(question);
        return market;
    }
}
