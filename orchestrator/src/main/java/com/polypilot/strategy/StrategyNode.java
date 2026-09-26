package com.polypilot.strategy;

import java.util.Set;

/**
 * One node of a strategy's entry/exit condition tree. See
 * {@link StrategyEvaluationService} for the two-phase evaluation flow this
 * supports: a single-threaded {@link #collectIndicatorRequests} pass over the
 * whole tree first, then a pure in-memory {@link #evaluate} pass once every
 * requested indicator has been computed.
 *
 * <p>Nodes are plain mutable classes (not {@code @Value}), by deliberate
 * exception to the project's "DTOs are immutable" convention — these are
 * domain nodes meant to be edited in place (e.g. by a future strategy
 * editor), not request/response DTOs. See the design doc's "Tree mutability"
 * decision and its caveat about concurrent mutation during an in-flight
 * evaluation.
 */
public abstract class StrategyNode {

    /**
     * Evaluates this node against already-computed indicator results. Pure
     * in-memory boolean logic — no I/O, no indicator computation happens
     * here.
     */
    public abstract boolean evaluate(EvaluationContext ctx);

    /**
     * Walks this node (and its children) adding every {@link IndicatorRequestKey}
     * it needs to {@code out}. Called once, single-threaded, before any
     * indicator is computed, so that computation can be deduplicated and
     * dispatched in parallel.
     */
    public abstract void collectIndicatorRequests(Set<IndicatorRequestKey> out);

    /**
     * True if this node (or any descendant) is a {@link MarketFieldCompareNode} — i.e. the tree
     * needs a {@link MarketFieldSnapshot} resolved before {@link #evaluate} can run. Checked once
     * per evaluation by {@link StrategyEvaluationService} to decide whether to dispatch
     * {@link MarketFieldResolver#resolve} at all.
     */
    public abstract boolean usesMarketFields();
}
