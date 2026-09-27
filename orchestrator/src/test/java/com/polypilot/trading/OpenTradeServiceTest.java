package com.polypilot.trading;

import com.polypilot.entity.Order;
import com.polypilot.entity.Position;
import com.polypilot.entity.Strategy;
import com.polypilot.enums.alert.ActionType;
import com.polypilot.enums.order.OrderStatus;
import com.polypilot.enums.order.OrderType;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.entity.Market;
import com.polypilot.market.enums.MarketOutcome;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.service.MarketItemSyncService;
import com.polypilot.repository.OrderRepository;
import com.polypilot.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link OpenTradeService}: {@link OpenTradeService#resolveFillPrice} (the
 * refresh-with-fallback price resolution slice) and {@link OpenTradeService#openTrade}'s full
 * order-construction / position-upsert flow. 
 *
 * <p>{@code marketId} is passed in explicitly by the caller ({@code StrategyService.runEvaluation},
 * which resolves it from the strategy's series) rather than read off the {@link Strategy} entity.
 */
@ExtendWith(MockitoExtension.class)
class OpenTradeServiceTest {

    private static final String MARKET_ID = "market-1";

    @Mock
    private MarketRepository marketRepository;
    @Mock
    private MarketItemSyncService marketItemSyncService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PositionRepository positionRepository;

    private OpenTradeService openTradeService;

    @BeforeEach
    void setUp() {
        openTradeService = new OpenTradeService(
                marketRepository, marketItemSyncService, orderRepository, positionRepository);
    }

    // --- resolveFillPrice -------------------------------------------------------------------

    @Test
    void refreshSucceeds_thenReadsTheFreshlyPersistedMarket() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        Market refreshedMarket = market(MarketStatus.OPEN, new BigDecimal("0.65"), new BigDecimal("0.35"));
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(refreshedMarket));

        MarketPriceResolution resolution = openTradeService.resolveFillPrice(strategy, MARKET_ID);

        InOrder order = inOrder(marketItemSyncService, marketRepository);
        order.verify(marketItemSyncService).refreshOpenMarket(MARKET_ID);
        order.verify(marketRepository).findById(MARKET_ID);
        assertThat(resolution.isSkipped()).isFalse();
        assertThat(resolution.getMarket()).isSameAs(refreshedMarket);
        assertThat(resolution.getPrice()).isEqualByComparingTo("0.65");
    }

    @Test
    void refreshThrows_fallsBackToWhateverIsAlreadyPersisted() {
        Strategy strategy = strategy(MarketOutcome.NO, new BigDecimal("100"));
        Market staleMarket = market(MarketStatus.OPEN, new BigDecimal("0.6"), new BigDecimal("0.4"));
        doThrow(new ResourceAccessException("Gamma unreachable"))
                .when(marketItemSyncService).refreshOpenMarket(MARKET_ID);
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(staleMarket));

        MarketPriceResolution resolution = openTradeService.resolveFillPrice(strategy, MARKET_ID);

        assertThat(resolution.isSkipped()).isFalse();
        assertThat(resolution.getPrice()).isEqualByComparingTo("0.4");
    }

    @Test
    void refreshThrowsIllegalStateException_alsoFallsBack() {
        // refreshOpenMarket itself throws IllegalStateException when the market row is missing
        // from its own (separate) persistence context — must be swallowed the same as any other
        // refresh failure, not just RestClient-flavored exceptions.
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        doThrow(new IllegalStateException("Market not found: " + MARKET_ID))
                .when(marketItemSyncService).refreshOpenMarket(MARKET_ID);
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.empty());

        MarketPriceResolution resolution = openTradeService.resolveFillPrice(strategy, MARKET_ID);

        assertThat(resolution.isSkipped()).isTrue();
        assertThat(resolution.getSkipReason()).contains("Market not found");
    }

    @Test
    void marketNoLongerExists_skipsWithReason() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.empty());

        MarketPriceResolution resolution = openTradeService.resolveFillPrice(strategy, MARKET_ID);

        assertThat(resolution.isSkipped()).isTrue();
        assertThat(resolution.getSkipReason()).isEqualTo("Market not found: " + MARKET_ID);
    }

    @Test
    void marketNotOpen_skipsWithReason() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.CLOSED, new BigDecimal("0.65"), new BigDecimal("0.35"))));

        MarketPriceResolution resolution = openTradeService.resolveFillPrice(strategy, MARKET_ID);

        assertThat(resolution.isSkipped()).isTrue();
        assertThat(resolution.getSkipReason()).isEqualTo("Market is not OPEN");
    }

    @Test
    void priceMissingForStrategysTokenSide_skipsWithReason() {
        Strategy strategy = strategy(MarketOutcome.NO, new BigDecimal("100"));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.65"), null)));

        MarketPriceResolution resolution = openTradeService.resolveFillPrice(strategy, MARKET_ID);

        assertThat(resolution.isSkipped()).isTrue();
        assertThat(resolution.getSkipReason()).isEqualTo("No price available for NO");
    }

    // --- openTrade: skip paths ---------------------------------------------------------------

    @Test
    void openTrade_returnsSkippedResult_whenPriceResolutionSkips_andTouchesNoOrderOrPositionRepo() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.empty());

        OpenTradeResult result = openTradeService.openTrade(strategy, MARKET_ID);

        assertThat(result.getAuditAction()).isEqualTo(ActionType.ORDER_SKIPPED);
        assertThat(result.getOrder()).isNull();
        assertThat(result.getPosition()).isNull();
        assertThat(result.getReasoning()).isEqualTo("Market not found: " + MARKET_ID);
        verifyNoInteractions(orderRepository, positionRepository);
    }

    @Test
    void openTrade_maxBetSizeNotPositive_skips_andTouchesNoOrderOrPositionRepo() {
        Strategy strategy = strategy(MarketOutcome.YES, BigDecimal.ZERO);
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.65"), new BigDecimal("0.35"))));

        OpenTradeResult result = openTradeService.openTrade(strategy, MARKET_ID);

        assertThat(result.getAuditAction()).isEqualTo(ActionType.ORDER_SKIPPED);
        assertThat(result.getReasoning()).contains("maxBetSize");
        verifyNoInteractions(orderRepository, positionRepository);
    }

    // --- openTrade: order construction --------------------------------------------------------

    @Test
    void openTrade_buildsAFilledOrder_withSizeComputedFromMaxBetSizeOverPrice_roundedHalfUp() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.65"), new BigDecimal("0.35"))));
        when(positionRepository.findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                strategy.getUserId(), MARKET_ID, MarketOutcome.YES, true, PositionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.saveAndFlush(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenTradeResult result = openTradeService.openTrade(strategy, MARKET_ID);

        assertThat(result.getAuditAction()).isEqualTo(ActionType.DRY_RUN_ORDER);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        Order order = orderCaptor.getValue();

        // 100 / 0.65 = 153.846153846... -> HALF_UP at scale 4 -> 153.8462
        assertThat(order.getSizeRequested()).isEqualByComparingTo("153.8462");
        assertThat(order.getSizeFilled()).isEqualByComparingTo("153.8462");
        assertThat(order.getPrice()).isEqualByComparingTo("0.65");
        assertThat(order.getStrategyId()).isEqualTo(strategy.getId());
        assertThat(order.getMarketId()).isEqualTo(MARKET_ID);
        assertThat(order.getUserId()).isEqualTo(strategy.getUserId());
        assertThat(order.getTokenSide()).isEqualTo(MarketOutcome.YES);
        assertThat(order.getOrderType()).isEqualTo(OrderType.GTC);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getIsDryRun()).isTrue();
        assertThat(order.getExternalOrderId()).isNull();
        assertThat(order.getFailureReason()).isNull();
        assertThat(order.getPlacedAt()).isEqualTo(order.getFilledAt());
    }

    // --- openTrade: position upsert -----------------------------------------------------------

    @Test
    void openTrade_noExistingOpenPosition_insertsANewOneFromTheFill() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("100"));
        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.65"), new BigDecimal("0.35"))));
        when(positionRepository.findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                strategy.getUserId(), MARKET_ID, MarketOutcome.YES, true, PositionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.saveAndFlush(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenTradeResult result = openTradeService.openTrade(strategy, MARKET_ID);

        Position position = result.getPosition();
        assertThat(position.getSize()).isEqualByComparingTo("153.8462");
        assertThat(position.getAvgEntryPrice()).isEqualByComparingTo("0.65");
        assertThat(position.getCurrentPrice()).isEqualByComparingTo("0.65");
        assertThat(position.getUnrealizedPnl()).isEqualByComparingTo("0");
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("0");
        assertThat(position.getStatus()).isEqualTo(PositionStatus.OPEN);
        assertThat(position.getIsDryRun()).isTrue();
        assertThat(position.getClosedAt()).isNull();
    }

    @Test
    void openTrade_existingOpenPosition_updatesWithWeightedAverageEntryPrice() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("50"));
        OffsetDateTime openedAt = OffsetDateTime.now().minusDays(1);
        Position existingPosition = Position.builder()
                .id(UUID.randomUUID())
                .marketId(MARKET_ID)
                .userId(strategy.getUserId())
                .tokenSide(MarketOutcome.YES)
                .size(new BigDecimal("10"))
                .avgEntryPrice(new BigDecimal("0.4000"))
                .currentPrice(new BigDecimal("0.4000"))
                .unrealizedPnl(BigDecimal.ZERO)
                .realizedPnl(new BigDecimal("5.0000"))
                .status(PositionStatus.OPEN)
                .isDryRun(true)
                .openedAt(openedAt)
                .build();

        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.5"), new BigDecimal("0.5"))));
        when(positionRepository.findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                strategy.getUserId(), MARKET_ID, MarketOutcome.YES, true, PositionStatus.OPEN))
                .thenReturn(Optional.of(existingPosition));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.saveAndFlush(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        // size = 50 / 0.5 = 100 exactly
        OpenTradeResult result = openTradeService.openTrade(strategy, MARKET_ID);

        Position position = result.getPosition();
        assertThat(position).isSameAs(existingPosition);
        assertThat(position.getSize()).isEqualByComparingTo("110");
        // (10 * 0.4 + 100 * 0.5) / 110 = 54 / 110 = 0.490909... -> HALF_UP scale 4 -> 0.4909
        assertThat(position.getAvgEntryPrice()).isEqualByComparingTo("0.4909");
        assertThat(position.getCurrentPrice()).isEqualByComparingTo("0.5");
        // (0.5 - 0.4909) * 110 = 1.0010
        assertThat(position.getUnrealizedPnl()).isEqualByComparingTo("1.0010");
        assertThat(position.getRealizedPnl()).isEqualByComparingTo("5.0000");
        assertThat(position.getStatus()).isEqualTo(PositionStatus.OPEN);
        assertThat(position.getOpenedAt()).isEqualTo(openedAt);
        verify(positionRepository, never()).save(any());
    }

    @Test
    void openTrade_positionInsertRacesAnotherTransaction_retriesAsUpdate() {
        Strategy strategy = strategy(MarketOutcome.YES, new BigDecimal("50"));
        Position racedWinner = Position.builder()
                .id(UUID.randomUUID())
                .marketId(MARKET_ID)
                .userId(strategy.getUserId())
                .tokenSide(MarketOutcome.YES)
                .size(new BigDecimal("10"))
                .avgEntryPrice(new BigDecimal("0.4000"))
                .realizedPnl(BigDecimal.ZERO)
                .status(PositionStatus.OPEN)
                .isDryRun(true)
                .openedAt(OffsetDateTime.now())
                .build();

        when(marketRepository.findById(MARKET_ID))
                .thenReturn(Optional.of(market(MarketStatus.OPEN, new BigDecimal("0.5"), new BigDecimal("0.5"))));
        when(positionRepository.findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                strategy.getUserId(), MARKET_ID, MarketOutcome.YES, true, PositionStatus.OPEN))
                // first lookup (before the insert attempt): nothing there yet
                // second lookup (after losing the race): the concurrently-inserted row
                .thenReturn(Optional.empty(), Optional.of(racedWinner));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.saveAndFlush(any(Position.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates uq_position_key"))
                .thenAnswer(inv -> inv.getArgument(0));

        OpenTradeResult result = openTradeService.openTrade(strategy, MARKET_ID);

        assertThat(result.getAuditAction()).isEqualTo(ActionType.DRY_RUN_ORDER);
        assertThat(result.getPosition()).isSameAs(racedWinner);
        assertThat(racedWinner.getSize()).isEqualByComparingTo("110");
        verify(positionRepository, times(2))
                .findByUserIdAndMarketIdAndTokenSideAndIsDryRunAndStatus(
                        strategy.getUserId(), MARKET_ID, MarketOutcome.YES, true, PositionStatus.OPEN);
        verify(positionRepository, times(2)).saveAndFlush(any(Position.class));
    }

    private static Strategy strategy(MarketOutcome tokenSide, BigDecimal maxBetSize) {
        return Strategy.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenSide(tokenSide)
                .orderType(OrderType.GTC)
                .maxBetSize(maxBetSize)
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
