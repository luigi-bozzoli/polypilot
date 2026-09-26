package com.polypilot.strategy;

import java.math.BigDecimal;
import java.util.Set;

/**
 * One side of a {@link CompareNode} comparison. Deliberately <b>not</b> a {@link StrategyNode}
 * subtype — it resolves to a {@link BigDecimal}, not a boolean, and is never a tree node in its
 * own right, only a value held by one. Mirrors {@link StrategyNode}'s two supporting hooks
 * ({@code resolve} standing in for {@code evaluate}, plus the same
 * {@code collectIndicatorRequests} pre-pass contract) so {@link CompareNode} can treat its two
 * operands uniformly regardless of which implementation backs them.
 *
 * <p>Two implementations today: {@link IndicatorOperand} (reads a pre-computed indicator output
 * out of the {@link EvaluationContext}) and {@link LiteralOperand} (a fixed value, contributing
 * nothing to indicator collection). A third kind (e.g. another market field) would only need to
 * add a new implementation here — {@link CompareNode} itself would not change.
 */
public interface ComparisonOperand {

    /**
     * Resolves this operand's value from already-computed indicator results. Pure in-memory — no
     * I/O, matching {@link StrategyNode#evaluate}'s contract.
     */
    BigDecimal resolve(EvaluationContext ctx);

    /**
     * Adds every {@link IndicatorRequestKey} this operand needs to {@code out}, so it's collected
     * before {@link StrategyEvaluationService} computes any indicator. A no-op for operands (like
     * {@link LiteralOperand}) that don't read an indicator.
     */
    void collectIndicatorRequests(Set<IndicatorRequestKey> out);
}
