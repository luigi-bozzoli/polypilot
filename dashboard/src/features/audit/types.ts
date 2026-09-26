import type { EngineDecision, MarketDecisionsResponse } from '../market/types'

/**
 * Audit-log domain types. `EngineDecision`/`MarketDecisionsResponse` already live in
 * `features/market/types.ts` (the market-detail page owned that contract first) — re-exported
 * here so `features/audit/` is a complete, self-contained home for the audit feed without
 * duplicating the shape.
 */
export type { EngineDecision, MarketDecisionsResponse }

/**
 * One row of the global audit feed, body of `GET /audit-logs` (paginated). Mirrors
 * `AuditLogView` (orchestrator) — wider than `EngineDecision`/`StrategyDecision` since it's not
 * pre-scoped to a market or strategy.
 */
export type AuditLogEntry = {
  id: string
  createdAt: string
  action: string
  isDryRun: boolean
  strategyId: string | null
  strategyName: string | null
  marketId: string | null
  marketQuestion: string | null
  orderId: string | null
  reasoning: string | null
  signals: string | null
}

/** Body of `GET /audit-logs`. Mirrors `AuditLogPageView` (orchestrator). */
export type AuditLogPage = {
  content: AuditLogEntry[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * One row of `GET /strategies/{id}/decisions` — the strategy-scoped slice of the audit feed.
 * Mirrors the backend's `StrategyDecisionView`. Unlike `EngineDecision`, there's no
 * `strategyId`/`strategyName` (the caller already knows which strategy) and no top-level `id`,
 * only `auditLogId`.
 */
export type StrategyDecisionKind =
  | 'DRY_RUN_ORDER'
  | 'LIVE_ORDER'
  | 'ORDER_SKIPPED'
  | 'RISK_BLOCKED'
  | 'SIGNAL_RECEIVED'
  | 'RULE_EVALUATED'
  | 'STRATEGY_EVALUATED'
  | (string & {})

export type StrategyDecision = {
  /** Event instant, ISO-8601 UTC. */
  occurredAt: string
  kind: StrategyDecisionKind
  /** One display-ready line, pre-formatted by the backend. */
  detail: string
  /** Audit-log row id, for the future audit-detail route. */
  auditLogId: string
}
