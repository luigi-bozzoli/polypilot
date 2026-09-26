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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One-item unit of work for {@link PositionCloseService}. Lives in its own bean so the
 * {@code REQUIRES_NEW} boundary is a real proxy hop — one position's failure never rolls back
 * another's, same isolation pattern as {@code MarketItemSyncService}/{@code OhlcStreamSyncService}.
 *
 * <p>Both the {@link Position} and its {@link Market} are re-read inside this method's own
 * transaction rather than passed in from the batch loop, so a race against another sweep (or a
 * manual close) is caught here instead of acting on a stale snapshot — same rule
 * {@code OpenTradeService.resolveFillPrice} follows for its own refresh-then-reload.
 *
 * <p>"Last known price at close" is read directly off {@code Market.upPrice}/{@code downPrice}:
 * {@code MarketMapper.updateEntity}'s {@code IGNORE} null-mapping strategy already keeps those
 * frozen at their last-synced value once Gamma stops reporting prices for a resolved market, so no
 * separate snapshot lookup is needed to capture "the price when it closed".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionClosureExecutor {

    private final PositionRepository positionRepository;
    private final MarketRepository marketRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * @return {@code true} if this call closed the position, {@code false} if it was skipped
     *         (already closed, market still OPEN, or no settlement price available yet).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean closePosition(UUID positionId) {
        Position position = positionRepository.findById(positionId).orElse(null);
        if (position == null || position.getStatus() != PositionStatus.OPEN) {
            log.debug("Position [{}] no longer OPEN, skipping", positionId);
            return false;
        }

        Market market = marketRepository.findById(position.getMarketId()).orElse(null);
        if (market == null) {
            log.warn("Position [{}] references unknown market [{}], skipping",
                    positionId, position.getMarketId());
            return false;
        }

        if (market.getStatus() == MarketStatus.OPEN) {
            return false; // not eligible yet — the common case on every sweep
        }

        BigDecimal settlementPrice = position.getTokenSide() == MarketOutcome.YES
                ? market.getUpPrice()
                : market.getDownPrice();

        if (settlementPrice == null) {
            log.warn("Market [{}] is {} but has no {} price yet — leaving position [{}] OPEN",
                    market.getId(), market.getStatus(), position.getTokenSide(), positionId);
            return false;
        }

        BigDecimal pnl = settlementPrice.subtract(position.getAvgEntryPrice()).multiply(position.getSize());
        OffsetDateTime now = OffsetDateTime.now();

        position.setRealizedPnl(position.getRealizedPnl().add(pnl));
        position.setUnrealizedPnl(BigDecimal.ZERO);
        position.setCurrentPrice(settlementPrice);
        position.setStatus(PositionStatus.CLOSED);
        position.setClosedAt(now);
        position.setUpdatedAt(now);
        positionRepository.save(position);

        auditLogRepository.save(buildAuditLog(position, market, settlementPrice, pnl, now));

        log.info("Position [{}] closed on market [{}] ({}) @ {} — realized P&L {}",
                positionId, market.getId(), market.getStatus(), settlementPrice, pnl);
        return true;
    }

    private AuditLog buildAuditLog(Position position, Market market, BigDecimal settlementPrice,
                                    BigDecimal pnl, OffsetDateTime now) {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("marketId", market.getId());
        signals.put("marketStatus", market.getStatus());
        signals.put("tokenSide", position.getTokenSide());
        signals.put("settlementPrice", settlementPrice);
        signals.put("avgEntryPrice", position.getAvgEntryPrice());
        signals.put("size", position.getSize());
        signals.put("realizedPnl", position.getRealizedPnl());

        return AuditLog.builder()
                .marketId(market.getId())
                .userId(position.getUserId())
                .action(ActionType.POSITION_CLOSED)
                .isDryRun(position.getIsDryRun())
                .signals(objectMapper.writeValueAsString(signals))
                .reasoning("Position closed on " + market.getStatus() + " market @ " + settlementPrice
                        + " (realized P&L " + pnl + ")")
                .createdAt(now)
                .build();
    }
}
