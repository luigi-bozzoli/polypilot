package com.polypilot.strategy;

import com.polypilot.indicator.service.IndicatorCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Evaluates a {@link StrategyNode} tree in two phases so that tree-walking stays pure in-memory
 * boolean logic with no I/O:
 *
 * <ol>
 *   <li>Single-threaded collection pass — snapshot of every distinct indicator request the tree
 *       needs, plus whether it needs a {@link MarketFieldSnapshot} at all
 *       ({@link StrategyNode#usesMarketFields}).</li>
 *   <li>Dispatch one async {@code calculate()} per distinct indicator request, and — only if the
 *       tree has at least one {@link MarketFieldCompareNode} — one {@link MarketFieldResolver#resolve}
 *       call, all onto the same bounded pool (both are blocking JPA I/O — see
 *       {@code StrategyExecutorConfig}), join all, then walk the tree against the resolved
 *       {@link EvaluationContext}.</li>
 * </ol>
 *
 * <p>Market fields resolve from {@code marketId}; indicators resolve from {@code binanceSymbol} —
 * the two are deliberately independent parameters (constraint from the strategy-node-market-fields
 * design), and {@link MarketFieldResolver#resolve} is called at most once per evaluation regardless
 * of how many {@link MarketFieldCompareNode}s the tree contains.
 *
 * <p>A failed indicator computation (e.g. not enough closed candles) or a failed market-field
 * resolution (e.g. unknown {@code marketId}) fails the whole evaluation via {@code .join()} — a
 * strategy can't be meaningfully evaluated with a missing input, so fail-fast beats silently
 * treating a missing indicator/market as {@code false}.
 */
@Service
@RequiredArgsConstructor
public class StrategyEvaluationService {

    private final IndicatorCalculationService indicatorCalculationService;
    private final MarketFieldResolver marketFieldResolver;
    private final ThreadPoolTaskExecutor indicatorExecutor;

    public boolean evaluate(StrategyNode tree, String binanceSymbol, String marketId) {
        Set<IndicatorRequestKey> requests = new HashSet<>();
        tree.collectIndicatorRequests(requests);
        boolean needsMarketFields = tree.usesMarketFields();

        Map<IndicatorRequestKey, CompletableFuture<Map<String, BigDecimal>>> indicatorFutures = requests.stream()
                .collect(toMap(identity(), key -> CompletableFuture.supplyAsync(
                        () -> indicatorCalculationService.calculate(binanceSymbol, key.getIndicatorKey(), key.getParams()),
                        indicatorExecutor)));

        CompletableFuture<MarketFieldSnapshot> marketFieldFuture = needsMarketFields
                ? CompletableFuture.supplyAsync(() -> marketFieldResolver.resolve(marketId), indicatorExecutor)
                : null;

        CompletableFuture.allOf(indicatorFutures.values().toArray(CompletableFuture[]::new)).join();
        Map<IndicatorRequestKey, Map<String, BigDecimal>> indicatorResults = indicatorFutures.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().join()));

        MarketFieldSnapshot marketFieldSnapshot = marketFieldFuture != null ? marketFieldFuture.join() : null;

        return tree.evaluate(new EvaluationContext(indicatorResults, marketFieldSnapshot));
    }
}
