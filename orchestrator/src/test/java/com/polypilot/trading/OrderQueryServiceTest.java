package com.polypilot.trading;

import com.polypilot.entity.Order;
import com.polypilot.entity.Strategy;
import com.polypilot.enums.order.OrderStatus;
import com.polypilot.market.dto.view.MarketOrderView;
import com.polypilot.market.dto.view.MarketOrdersView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.OrderRepository;
import com.polypilot.strategy.dto.view.StrategyOrderView;
import com.polypilot.strategy.repository.StrategyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test on the service directly (no Spring context), matching this codebase's convention —
 * see {@code AuditLogQueryServiceTest}'s Tests section.
 */
class OrderQueryServiceTest {

    private final OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
    private final MarketRepository marketRepository = Mockito.mock(MarketRepository.class);
    private final StrategyRepository strategyRepository = Mockito.mock(StrategyRepository.class);
    private final OrderQueryService service =
            new OrderQueryService(orderRepository, marketRepository, strategyRepository);

    private final UUID userId = UUID.randomUUID();
    private final UUID strategyId = UUID.randomUUID();

    @Test
    void getStrategyOrders_mapsAFilledOrderAndResolvesTheMarketQuestion() {
        Order order = order(OrderStatus.FILLED, "0x4f2a9c");
        when(orderRepository.findByUserIdAndStrategyId(eq(userId), eq(strategyId), any()))
                .thenReturn(pageOf(order));
        when(marketRepository.findAllById(List.of("0x4f2a9c")))
                .thenReturn(List.of(market("0x4f2a9c", "Will BTC close above $110K?")));

        List<StrategyOrderView> result = service.getStrategyOrders(userId, strategyId, 20);

        assertThat(result).hasSize(1);
        StrategyOrderView view = result.get(0);
        assertThat(view.getId()).isEqualTo(order.getId());
        assertThat(view.getMarketId()).isEqualTo("0x4f2a9c");
        assertThat(view.getMarketQuestion()).isEqualTo("Will BTC close above $110K?");
        assertThat(view.getTokenSide()).isEqualTo(MarketOutcome.YES);
        assertThat(view.getSizeRequested()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(view.getSizeFilled()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(view.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(0.71));
        assertThat(view.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(view.getIsDryRun()).isTrue();
    }

    @Test
    void getStrategyOrders_handlesNonFilledStatusesGenerically() {
        Order order = order(OrderStatus.CANCELLED, "0x4f2a9c");
        when(orderRepository.findByUserIdAndStrategyId(eq(userId), eq(strategyId), any()))
                .thenReturn(pageOf(order));
        when(marketRepository.findAllById(List.of("0x4f2a9c"))).thenReturn(List.of());

        List<StrategyOrderView> result = service.getStrategyOrders(userId, strategyId, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void getStrategyOrders_fallsBackToTheRawMarketIdWhenTheMarketCannotBeResolved() {
        Order order = order(OrderStatus.FILLED, "0xunknown");
        when(orderRepository.findByUserIdAndStrategyId(eq(userId), eq(strategyId), any()))
                .thenReturn(pageOf(order));
        when(marketRepository.findAllById(List.of("0xunknown"))).thenReturn(List.of());

        List<StrategyOrderView> result = service.getStrategyOrders(userId, strategyId, 20);

        assertThat(result.get(0).getMarketQuestion()).isEqualTo("0xunknown");
    }

    @Test
    void getStrategyOrders_returnsEmptyListForAnUnownedOrUnknownStrategyWithNoException() {
        when(orderRepository.findByUserIdAndStrategyId(eq(userId), eq(strategyId), any()))
                .thenReturn(pageOf());

        List<StrategyOrderView> result = service.getStrategyOrders(userId, strategyId, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void getStrategyOrders_buildsANewestFirstPageableWithTheGivenLimit() {
        when(orderRepository.findByUserIdAndStrategyId(eq(userId), eq(strategyId), any()))
                .thenReturn(pageOf());

        service.getStrategyOrders(userId, strategyId, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(orderRepository).findByUserIdAndStrategyId(eq(userId), eq(strategyId), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "placedAt"));
    }

    @Test
    void getMarketOrders_mapsAFilledOrderAndResolvesTheStrategyName() {
        Order order = order(OrderStatus.FILLED, "0x4f2a9c");
        when(orderRepository.findByUserIdAndMarketId(eq(userId), eq("0x4f2a9c"), any()))
                .thenReturn(pageOf(order));
        when(strategyRepository.findAllById(List.of(strategyId)))
                .thenReturn(List.of(strategy(strategyId, "BTC Bullish Momentum")));

        MarketOrdersView result = service.getMarketOrders(userId, "0x4f2a9c", 20);

        assertThat(result.getMarketId()).isEqualTo("0x4f2a9c");
        assertThat(result.getOrders()).hasSize(1);
        MarketOrderView view = result.getOrders().get(0);
        assertThat(view.getId()).isEqualTo(order.getId());
        assertThat(view.getStrategyId()).isEqualTo(strategyId);
        assertThat(view.getStrategyName()).isEqualTo("BTC Bullish Momentum");
        assertThat(view.getTokenSide()).isEqualTo(MarketOutcome.YES);
        assertThat(view.getSizeRequested()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(view.getSizeFilled()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(view.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(0.71));
        assertThat(view.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(view.getIsDryRun()).isTrue();
    }

    @Test
    void getMarketOrders_fallsBackToAnEmDashWhenTheStrategyCannotBeResolved() {
        Order order = order(OrderStatus.FILLED, "0x4f2a9c");
        when(orderRepository.findByUserIdAndMarketId(eq(userId), eq("0x4f2a9c"), any()))
                .thenReturn(pageOf(order));
        when(strategyRepository.findAllById(List.of(strategyId))).thenReturn(List.of());

        MarketOrdersView result = service.getMarketOrders(userId, "0x4f2a9c", 20);

        assertThat(result.getOrders().get(0).getStrategyName()).isEqualTo("—");
    }

    @Test
    void getMarketOrders_returnsEmptyListForAnUnownedOrUnknownMarketWithNoException() {
        when(orderRepository.findByUserIdAndMarketId(eq(userId), eq("0x4f2a9c"), any()))
                .thenReturn(pageOf());

        MarketOrdersView result = service.getMarketOrders(userId, "0x4f2a9c", 20);

        assertThat(result.getOrders()).isEmpty();
    }

    @Test
    void getMarketOrders_buildsANewestFirstPageableWithTheGivenLimit() {
        when(orderRepository.findByUserIdAndMarketId(eq(userId), eq("0x4f2a9c"), any()))
                .thenReturn(pageOf());

        service.getMarketOrders(userId, "0x4f2a9c", 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(orderRepository).findByUserIdAndMarketId(eq(userId), eq("0x4f2a9c"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "placedAt"));
    }

    private Strategy strategy(UUID id, String name) {
        Strategy strategy = new Strategy();
        strategy.setId(id);
        strategy.setName(name);
        return strategy;
    }

    private Order order(OrderStatus status, String marketId) {
        OffsetDateTime now = OffsetDateTime.now();
        return Order.builder()
                .id(UUID.randomUUID())
                .strategyId(strategyId)
                .marketId(marketId)
                .userId(userId)
                .tokenSide(MarketOutcome.YES)
                .sizeRequested(BigDecimal.valueOf(50))
                .sizeFilled(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(0.71))
                .status(status)
                .isDryRun(true)
                .placedAt(now)
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

    private org.springframework.data.domain.Page<Order> pageOf(Order... orders) {
        return new org.springframework.data.domain.PageImpl<>(List.of(orders));
    }
}
