package com.polypilot.strategy.service;

import com.polypilot.entity.AuditLog;
import com.polypilot.entity.Strategy;
import com.polypilot.enums.alert.ActionType;
import com.polypilot.market.entity.Market;
import com.polypilot.market.entity.Series;
import com.polypilot.market.enums.MarketStatus;
import com.polypilot.market.repository.MarketRepository;
import com.polypilot.market.repository.SeriesRepository;
import com.polypilot.repository.AuditLogRepository;
import com.polypilot.repository.OrderRepository;
import com.polypilot.repository.StrategyOrderCountView;
import com.polypilot.strategy.StrategyEvaluationService;
import com.polypilot.strategy.StrategyNode;
import com.polypilot.strategy.dto.request.CreateStrategyRequest;
import com.polypilot.strategy.dto.request.UpdateStrategyRequest;
import com.polypilot.strategy.dto.view.StrategyView;
import com.polypilot.strategy.mapper.RuleTreeMapper;
import com.polypilot.strategy.mapper.StrategyViewMapper;
import com.polypilot.strategy.repository.StrategyRepository;
import com.polypilot.strategy.scheduler.StrategyScheduler;
import com.polypilot.trading.OpenTradeResult;
import com.polypilot.trading.OpenTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final StrategyViewMapper strategyViewMapper;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final RuleTreeMapper ruleTreeMapper;
    private final StrategyRequestValidator strategyRequestValidator;
    private final StrategyScheduler strategyScheduler;
    private final StrategyEvaluationService strategyEvaluationService;
    private final OpenTradeService openTradeService;
    private final ObjectMapper objectMapper;
    private final SeriesRepository seriesRepository;
    private final MarketRepository marketRepository;

    @Transactional
    public StrategyView create(CreateStrategyRequest request, UUID userId) {
        strategyRequestValidator.validate(
                request.getTokenSide(),
                request.getMaxBetSize(),
                request.getMaxDailyExposure(),
                request.getCronExpression(),
                request.getRuleTree());

        if (strategyRepository.existsByNameAndUserIdAndDeletedAtIsNull(request.getName(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A strategy named \"" + request.getName() + "\" already exists");
        }

        Series series = resolveSeries(request.getSeriesId());

        Strategy strategy = Strategy.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .enabled(true)
                .dryRun(request.getDryRun())
                .cronExpression(request.getCronExpression())
                .series(series)
                .tokenSide(request.getTokenSide())
                .orderType(request.getOrderType())
                .maxBetSize(request.getMaxBetSize())
                .maxDailyExposure(request.getMaxDailyExposure())
                .stopLossThreshold(request.getStopLossThreshold())
                .ruleTree(ruleTreeMapper.serializeRuleTree(request.getRuleTree()))
                .build();

        Strategy saved = strategyRepository.saveAndFlush(strategy);
        // A strategy is enabled by default (dryRun defaults to true, so it's still safe), so this
        // registers it with the scheduler immediately.
        strategyScheduler.reschedule(saved);
        return strategyViewMapper.toView(saved);
    }

    @Transactional
    public StrategyView update(UUID id, UUID userId, UpdateStrategyRequest request) {
        Strategy strategy = strategyRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Strategy not found: " + id));

        strategyRequestValidator.validate(
                request.getTokenSide(),
                request.getMaxBetSize(),
                request.getMaxDailyExposure(),
                request.getCronExpression(),
                request.getRuleTree());

        if (strategyRepository.existsByNameAndUserIdAndIdNotAndDeletedAtIsNull(request.getName(), userId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A strategy named \"" + request.getName() + "\" already exists");
        }

        strategy.setName(request.getName());
        strategy.setDescription(request.getDescription());
        strategy.setDryRun(request.getDryRun());
        strategy.setCronExpression(request.getCronExpression());
        strategy.setSeries(resolveSeries(request.getSeriesId()));
        strategy.setTokenSide(request.getTokenSide());
        strategy.setOrderType(request.getOrderType());
        strategy.setMaxBetSize(request.getMaxBetSize());
        strategy.setMaxDailyExposure(request.getMaxDailyExposure());
        strategy.setStopLossThreshold(request.getStopLossThreshold());
        strategy.setRuleTree(ruleTreeMapper.serializeRuleTree(request.getRuleTree()));

        Strategy saved = strategyRepository.saveAndFlush(strategy);
        // Re-syncs scheduling state unconditionally rather than diffing old vs. new cron/enabled
        // — reschedule() is idempotent (cancels any existing future first), so this is correct
        // whether cron changed, enabled toggled, both, or neither.
        strategyScheduler.reschedule(saved);
        return strategyViewMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public List<StrategyView> listStrategies(UUID userId) {
        List<StrategyView> views = strategyViewMapper.toViews(strategyRepository.findAllByUserIdAndDeletedAtIsNull(userId));
        applyTradeCounts(views);
        return views;
    }

    @Transactional(readOnly = true)
    public StrategyView getStrategy(UUID id, UUID userId) {
        Strategy strategy = strategyRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Strategy not found: " + id));

        return strategyViewMapper.toView(strategy);
    }


    @Transactional
    public void delete(UUID id, UUID userId) {
        Strategy strategy = strategyRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Strategy not found: " + id));

        if (orderRepository.existsByStrategyId(id) || auditLogRepository.existsByStrategyId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete strategy \"" + strategy.getName() + "\" — it has order or audit history");
        }

        strategy.setDeletedAt(OffsetDateTime.now());
        strategyRepository.save(strategy);
        strategyScheduler.unregister(id);
    }

    /**
     * Flips a strategy's {@code enabled} flag and syncs the scheduler to match — the single
     * entry point for enabling/disabling a strategy after creation, backing
     * {@code PUT /strategies/{id}/enabled}.
     */
    @Transactional
    public StrategyView setEnabled(UUID id, UUID userId, boolean enabled) {
        Strategy strategy = strategyRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Strategy not found: " + id));

        strategy.setEnabled(enabled);
        Strategy saved = strategyRepository.saveAndFlush(strategy);
        strategyScheduler.reschedule(saved);
        return strategyViewMapper.toView(saved);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runEvaluation(UUID strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId).orElse(null);

        if (strategy == null || strategy.getDeletedAt() != null || !Boolean.TRUE.equals(strategy.getEnabled())) {
            log.info("Skipping evaluation for strategy [{}] — no longer enabled", strategyId);
            return;
        }

        // Resolved fresh every tick, never cached on the entity — a strategy is attached to a
        // series, not a fixed market, so its evaluation target follows the series's current OPEN
        // market and linked asset across rollovers.
        Series series = strategy.getSeries();
        String binanceSymbol = series.getAsset() != null ? series.getAsset().getBinanceSymbol() : null;
        String marketId = marketRepository
                .findFirstBySeriesIdAndStatusOrderByCreatedAtDesc(series.getId(), MarketStatus.OPEN)
                .map(Market::getId)
                .orElse(null);

        if (binanceSymbol == null || marketId == null) {
            log.warn("Strategy [{}] is enabled but its series [{}] has no resolvable asset/open market — skipping",
                    strategyId, series.getId());
            return;
        }

        StrategyNode tree = ruleTreeMapper.parseRuleTree(strategy.getRuleTree());
        if (tree == null) {
            log.error("Strategy [{}] has an unparseable rule_tree — skipping evaluation", strategyId);
            persistAuditLog(strategy, null, "Rule tree failed to parse — see server logs", marketId, binanceSymbol);
            return;
        }

        try {
            boolean result = strategyEvaluationService.evaluate(tree, binanceSymbol, marketId);
            persistAuditLog(strategy, result, null, marketId, binanceSymbol);

            if (result) {
                OpenTradeResult tradeResult = openTradeService.openTrade(strategy, marketId);
                persistTradeAuditLog(strategy, tradeResult, marketId);
            }
        } catch (Exception ex) {
            log.error("Evaluation failed for strategy [{}]", strategyId, ex);
            persistAuditLog(strategy, null, "Evaluation failed: " + ex.getMessage(), marketId, binanceSymbol);
        }
    }

    /**
     * Batch-fetches each view's total order count in one grouped query and sets it in place —
     * avoids an N+1 over the strategy list. Same shape as {@code OrderQueryService}'s
     * batch-lookup-then-merge helpers.
     */
    private void applyTradeCounts(List<StrategyView> views) {
        List<UUID> ids = views.stream().map(StrategyView::getId).toList();
        if (ids.isEmpty()) {
            return;
        }

        Map<UUID, Long> countsById = orderRepository.countByStrategyIdIn(ids).stream()
                .collect(Collectors.toMap(StrategyOrderCountView::getStrategyId, StrategyOrderCountView::getCount));

        for (StrategyView view : views) {
            view.setTradeCount(countsById.getOrDefault(view.getId(), 0L).intValue());
        }
    }

    /**
     * Looks up a series by id and confirms it has a linked {@code Ticker} asset — required to
     * resolve a Binance symbol for evaluation. Shared by {@link #create} and {@link #update}.
     */
    private Series resolveSeries(UUID seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Series not found: " + seriesId));

        if (series.getAsset() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Series \"" + series.getTitle() + "\" has no linked asset — cannot determine a Binance symbol for evaluation");
        }

        return series;
    }

    private void persistAuditLog(Strategy strategy, Boolean result, String failureReason, String marketId, String binanceSymbol) {
        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("marketId", marketId);
        signals.put("binanceSymbol", binanceSymbol);
        signals.put("result", result);
        if (failureReason != null) {
            signals.put("error", failureReason);
        }

        AuditLog auditLog = AuditLog.builder()
                .strategyId(strategy.getId())
                .marketId(marketId)
                .userId(strategy.getUserId())
                .action(ActionType.STRATEGY_EVALUATED)
                .isDryRun(strategy.getDryRun())
                .signals(objectMapper.writeValueAsString(signals))
                .reasoning(failureReason != null ? failureReason : "Strategy evaluated to " + result)
                .createdAt(OffsetDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Second audit_logs row written only when evaluation returned {@code true} and a trade was
     * attempted — {@code action} is {@link com.polypilot.enums.alert.ActionType#DRY_RUN_ORDER} on
     * a successful (simulated) fill or {@link com.polypilot.enums.alert.ActionType#ORDER_SKIPPED}
     * when a pre-flight check stopped it. {@code isDryRun} is hardcoded {@code true} here rather
     * than read from {@code strategy.getDryRun()} (unlike {@link #persistAuditLog}) — this row
     * records the outcome of a flow that only ever produces simulated trades (no
     * {@code POLYPILOT_LIVE_MODE} exists yet), so it can legitimately differ from the
     * {@code STRATEGY_EVALUATED} row's {@code isDryRun} for the same tick when a strategy has
     * {@code dryRun=false}.
     */
    private void persistTradeAuditLog(Strategy strategy, OpenTradeResult tradeResult, String marketId) {
        AuditLog auditLog = AuditLog.builder()
                .strategyId(strategy.getId())
                .marketId(marketId)
                .orderId(tradeResult.getOrder() != null ? tradeResult.getOrder().getId() : null)
                .userId(strategy.getUserId())
                .action(tradeResult.getAuditAction())
                .isDryRun(true)
                .reasoning(tradeResult.getReasoning())
                .createdAt(OffsetDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
