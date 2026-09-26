package com.polypilot.trading;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batch driver for the position-close sweep. Each open position is delegated to
 * {@link PositionClosureExecutor} so it runs in its own {@code REQUIRES_NEW} transaction — one bad
 * position is logged and skipped, the rest of the batch continues untouched. Same pattern as
 * {@code MarketSyncService} driving {@code MarketItemSyncService}.
 *
 * <p>Deliberately does not pre-filter by market status here: whether a position is eligible (its
 * market is no longer OPEN) is re-checked inside {@link PositionClosureExecutor}'s own transaction
 * against the freshest row, not against a snapshot taken at the start of this sweep.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionCloseService {

    private final PositionRepository positionRepository;
    private final PositionClosureExecutor positionClosureExecutor;

    public void closeEligiblePositions() {
        List<Position> openPositions = positionRepository.findAllByStatus(PositionStatus.OPEN);

        int closed = 0;
        for (Position position : openPositions) {
            try {
                if (positionClosureExecutor.closePosition(position.getId())) {
                    closed++;
                }
            } catch (Exception ex) {
                log.error("Failed to close position [{}], skipping", position.getId(), ex);
            }
        }

        log.info("Position-close sweep finished: {} closed out of {} open", closed, openPositions.size());
    }
}
