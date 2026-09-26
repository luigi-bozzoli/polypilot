package com.polypilot.trading;

import com.polypilot.entity.Position;
import com.polypilot.enums.order.PositionStatus;
import com.polypilot.market.entity.Market;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.repository.PositionRepository;
import com.polypilot.trading.dto.view.PositionView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-side query backing the Overview "Open positions" widget — the caller's open positions,
 * newest-opened first. Same batch-lookup-then-merge shape as {@code OrderQueryService}: one
 * query for every distinct {@code marketId} on the page, avoiding N+1.
 */
@Service
@RequiredArgsConstructor
public class PositionQueryService {

    private final PositionRepository positionRepository;
    private final MarketRepository marketRepository;

    @Transactional(readOnly = true)
    public List<PositionView> getOpenPositions(UUID userId, int limit) {
        List<Position> positions = positionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, PositionStatus.OPEN);

        Map<String, String> marketQuestions = marketQuestionsById(positions);

        return positions.stream()
                .limit(limit)
                .map(position -> PositionView.builder()
                        .id(position.getId())
                        .marketId(position.getMarketId())
                        .marketQuestion(marketQuestions.getOrDefault(position.getMarketId(), position.getMarketId()))
                        .tokenSide(position.getTokenSide())
                        .size(position.getSize())
                        .avgEntryPrice(position.getAvgEntryPrice())
                        .unrealizedPnl(position.getUnrealizedPnl())
                        .openedAt(position.getOpenedAt())
                        .build())
                .toList();
    }

    /** Batch lookup — one query for every distinct {@code marketId}, avoiding N+1. */
    private Map<String, String> marketQuestionsById(List<Position> positions) {
        List<String> marketIds = positions.stream()
                .map(Position::getMarketId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (marketIds.isEmpty()) {
            return Map.of();
        }

        return marketRepository.findAllById(marketIds).stream()
                .collect(Collectors.toMap(Market::getId, Market::getQuestion));
    }
}
