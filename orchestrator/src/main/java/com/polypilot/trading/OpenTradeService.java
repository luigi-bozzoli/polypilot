package com.polypilot.trading;

import com.polypilot.entity.Order;
import com.polypilot.entity.Position;
import com.polypilot.entity.Strategy;
import com.polypilot.enums.order.OrderStatus;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.service.MarketItemSyncService;
import com.polypilot.repository.OrderRepository;
import com.polypilot.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Opens a simulated (dry-run) trade when a strategy's rule tree evaluates to {@code true}. One {@link Order} row (always an instant,
 * fully-filled simulated fill — {@code order_type} is not modeled) and one upserted {@link
 * Position} row per successful attempt, or an {@code ORDER_SKIPPED} result with no rows written
 * when a pre-flight check fails.
 *
 * <p>Deliberately has no {@code @Transactional} of its own: 
 * The order insert, position upsert, and the caller's audit-log write must commit or roll back
 * together as one unit, so this runs inside whatever transaction the caller ({@code
 * StrategyService.runEvaluation}, {@code REQUIRES_NEW}) already has open.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenTradeService {

    /** Both prices (NUMERIC(6,4)) and sizes (NUMERIC(18,4)) are persisted at 4 decimal places. */
    private static final int SCALE = 4;

    private final MarketRepository marketRepository;
    private final MarketItemSyncService marketItemSyncService;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;

    public OpenTradeResult openTrade(Strategy strategy, String marketId) {
        MarketPriceResolution resolution = resolveFillPrice(strategy, marketId);
        if (resolution.isSkipped()) {
            return OpenTradeResult.skipped(resolution.getSkipReason());
        }

        BigDecimal maxBetSize = strategy.getMaxBetSize();
        if (maxBetSize == null || maxBetSize.compareTo(BigDecimal.ZERO) <= 0) {
            return OpenTradeResult.skipped("strategy.maxBetSize must be greater than 0");
        }

        Market market = resolution.getMarket();
        BigDecimal price = resolution.getPrice();
        BigDecimal size = maxBetSize.divide(price, SCALE, RoundingMode.HALF_UP);

        Order order = orderRepository.saveAndFlush(buildFilledOrder(strategy, market, price, size));
        Position position = upsertPosition(order);

        String reasoning = "Filled " + size + " @ " + price + " (" + strategy.getTokenSide() + ")";
        return OpenTradeResult.opened(order, position, reasoning);
    }

    /**
     Refreshes the strategy's (series-resolved) market synchronously, falling back to the
     persisted values if the refresh fails, then resolves the reference price ({@code upPrice}
     for {@code YES}, {@code downPrice} for {@code NO}).

     <p>The {@link Market} is deliberately loaded only after the refresh. Since
     {@code refreshOpenMarket} runs in a separate {@code REQUIRES_NEW} persistence context,
     loading the row beforehand could leave it cached in the caller's context and cause
     {@code findById} to return stale data. This ensures the load reflects the refreshed values,
     or the existing persisted values when the refresh fails.
     */
    MarketPriceResolution resolveFillPrice(Strategy strategy, String marketId) {
        try {
            marketItemSyncService.refreshOpenMarket(marketId);
        } catch (RuntimeException ex) {
            log.warn("Market refresh failed for [{}], falling back to last-synced price", marketId, ex);
        }

        Optional<Market> marketOpt = marketRepository.findById(marketId);
        if (marketOpt.isEmpty()) {
            return MarketPriceResolution.skip("Market not found: " + marketId);
        }

        Market market = marketOpt.get();
        if (market.getStatus() != MarketStatus.OPEN) {
            return MarketPriceResolution.skip("Market is not OPEN");
        }

        BigDecimal price = strategy.getTokenSide() == MarketOutcome.YES
                ? market.getUpPrice()
                : market.getDownPrice();

        if (price == null) {
            return MarketPriceResolution.skip("No price available for " + strategy.getTokenSide());
        }

        return MarketPriceResolution.resolved(market, price);
    }

    private Order buildFilledOrder(Strategy strategy, Market market, BigDecimal price, BigDecimal size) {
        OffsetDateTime now = OffsetDateTime.now();
        return Order.builder()
                .strategyId(strategy.getId())
                .marketId(market.getId())
                .userId(strategy.getUserId())
                .externalOrderId(null)
                .tokenSide(strategy.getTokenSide())
                .orderType(strategy.getOrderType())
                .sizeRequested(size)
                .sizeFilled(size)
                .price(price)
                .status(OrderStatus.FILLED)
                .isDryRun(true)
                .failureReason(null)
                .placedAt(now)
                .filledAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     Looks up the strategy's current OPEN dry-run position for the market/side,
     updating it with a weighted-average entry price or inserting a new position.
     <p>{@code saveAndFlush} is intentional: it forces the unique-position constraint
     to be checked here, so concurrent insert failures are detected inside this method
     rather than being deferred to a later transaction flush.

     <p><b>Known limitation:</b> the retry-as-update path assumes the JPA provider allows
     another {@code saveAndFlush} after a constraint violation. Some providers mark the
     transaction rollback-only after a flush failure, so this behavior must be verified
     with the project's Hibernate setup under real concurrent load. The race is considered
     unlikely but is not impossible.
     */
    private Position upsertPosition(Order order) {
        Optional<Position> existing = positionRepository.findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                order.getUserId(), order.getMarketId(), order.getTokenSide(), true, PositionStatus.OPEN);

        if (existing.isPresent()) {
            Position position = existing.get();
            applyFill(position, order);
            return positionRepository.saveAndFlush(position);
        }

        try {
            return positionRepository.saveAndFlush(newPosition(order));
        } catch (DataIntegrityViolationException ex) {
            Position raced = positionRepository.findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                            order.getUserId(), order.getMarketId(), order.getTokenSide(), true, PositionStatus.OPEN)
                    .orElseThrow(() -> ex);
            applyFill(raced, order);
            return positionRepository.saveAndFlush(raced);
        }
    }

    private Position newPosition(Order order) {
        OffsetDateTime now = OffsetDateTime.now();
        return Position.builder()
                .marketId(order.getMarketId())
                .userId(order.getUserId())
                .tokenSide(order.getTokenSide())
                .size(order.getSizeFilled())
                .avgEntryPrice(order.getPrice())
                .currentPrice(order.getPrice())
                .unrealizedPnl(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .status(PositionStatus.OPEN)
                .isDryRun(true)
                .openedAt(now)
                .closedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** Mutates {@code position} in place to fold in {@code order}'s fill — the update path only. */
    private void applyFill(Position position, Order order) {
        BigDecimal oldSize = position.getSize();
        BigDecimal newSize = oldSize.add(order.getSizeFilled());
        BigDecimal newAvgEntryPrice = oldSize.multiply(position.getAvgEntryPrice())
                .add(order.getSizeFilled().multiply(order.getPrice()))
                .divide(newSize, SCALE, RoundingMode.HALF_UP);

        position.setSize(newSize);
        position.setAvgEntryPrice(newAvgEntryPrice);
        position.setCurrentPrice(order.getPrice());
        position.setUnrealizedPnl(order.getPrice().subtract(newAvgEntryPrice).multiply(newSize));
        position.setUpdatedAt(OffsetDateTime.now());
    }
}
