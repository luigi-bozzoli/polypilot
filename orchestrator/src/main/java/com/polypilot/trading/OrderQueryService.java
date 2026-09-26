package com.polypilot.trading;

import com.polypilot.entity.Order;
import com.polypilot.entity.Strategy;
import com.polypilot.market.dto.view.MarketOrderView;
import com.polypilot.market.dto.view.MarketOrdersView;
import com.polypilot.market.entity.Market;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.OrderRepository;
import com.polypilot.strategy.dto.view.StrategyOrderView;
import com.polypilot.strategy.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-side query backing {@code StrategyRecentOrdersCard} — the strategy-scoped slice of
 * {@code orders}, newest first. Pure fan-in over {@link OrderRepository}/{@link MarketRepository},
 * same isolation/ownership pattern as {@code AuditLogQueryService.getStrategyDecisions}: no
 * ownership check against {@code Strategy} itself, the {@code userId} filter inside the repository
 * query does that — an unknown/unowned strategy id simply yields an empty list, not a 404.
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final MarketRepository marketRepository;
    private final StrategyRepository strategyRepository;

    @Transactional(readOnly = true)
    public List<StrategyOrderView> getStrategyOrders(UUID userId, UUID strategyId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<Order> orders = orderRepository.findByUserIdAndStrategyId(userId, strategyId, pageable);

        Map<String, String> marketQuestions = marketQuestionsById(orders.getContent());

        return orders.getContent().stream()
                .map(order -> StrategyOrderView.builder()
                        .id(order.getId())
                        .placedAt(order.getPlacedAt())
                        .marketId(order.getMarketId())
                        .marketQuestion(marketQuestions.getOrDefault(order.getMarketId(), order.getMarketId()))
                        .tokenSide(order.getTokenSide())
                        .sizeRequested(order.getSizeRequested())
                        .sizeFilled(order.getSizeFilled())
                        .price(order.getPrice())
                        .status(order.getStatus())
                        .isDryRun(order.getIsDryRun())
                        .build())
                .toList();
    }

    /**
     * Market-scoped slice of {@code orders}, newest first — backs {@code RecentOrdersCard} on the
     * market detail page. Same ownership pattern as {@link #getStrategyOrders}: an unknown/unowned
     * market id simply yields an empty list.
     */
    @Transactional(readOnly = true)
    public MarketOrdersView getMarketOrders(UUID userId, String marketId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<Order> orders = orderRepository.findByUserIdAndMarketId(userId, marketId, pageable);

        Map<UUID, String> strategyNames = strategyNamesById(orders.getContent());

        List<MarketOrderView> views = orders.getContent().stream()
                .map(order -> MarketOrderView.builder()
                        .id(order.getId())
                        .placedAt(order.getPlacedAt())
                        .strategyId(order.getStrategyId())
                        .strategyName(strategyNames.getOrDefault(order.getStrategyId(), "—"))
                        .tokenSide(order.getTokenSide())
                        .sizeRequested(order.getSizeRequested())
                        .sizeFilled(order.getSizeFilled())
                        .price(order.getPrice())
                        .status(order.getStatus())
                        .isDryRun(order.getIsDryRun())
                        .build())
                .toList();

        return MarketOrdersView.builder().marketId(marketId).orders(views).build();
    }

    /** Batch lookup — one query for every distinct {@code marketId} on the page, avoiding N+1. */
    private Map<String, String> marketQuestionsById(List<Order> orders) {
        List<String> marketIds = orders.stream()
                .map(Order::getMarketId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (marketIds.isEmpty()) {
            return Map.of();
        }

        return marketRepository.findAllById(marketIds).stream()
                .collect(Collectors.toMap(Market::getId, Market::getQuestion));
    }

    /** Batch lookup — one query for every distinct {@code strategyId} on the page, avoiding N+1. */
    private Map<UUID, String> strategyNamesById(List<Order> orders) {
        List<UUID> strategyIds = orders.stream()
                .map(Order::getStrategyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (strategyIds.isEmpty()) {
            return Map.of();
        }

        return strategyRepository.findAllById(strategyIds).stream()
                .collect(Collectors.toMap(Strategy::getId, Strategy::getName));
    }
}
