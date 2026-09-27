import { authGet } from '../../lib/http'
import type { AuditLogPage, MarketDecisionsResponse, StrategyDecision } from './types'

/**
 * Global audit feed, newest first, paginated. Backed by `GET /audit-logs`. Used by the Overview
 * "Recent engine decisions" widget — the market-/strategy-scoped fetchers below are thinner
 * slices of this same underlying query.
 */
export function fetchAuditLogs(size = 20): Promise<AuditLogPage> {
  const qs = new URLSearchParams({ size: String(size) })
  return authGet<AuditLogPage>(`/api/audit-logs?${qs}`)
}

/**
 * Market-scoped slice of the audit feed, newest first. Backed by `GET /market/{marketId}/decisions`.
 */
export function fetchMarketDecisions(marketId: string, limit = 20): Promise<MarketDecisionsResponse> {
  const qs = new URLSearchParams({ limit: String(limit) })
  return authGet<MarketDecisionsResponse>(`/api/market/${marketId}/decisions?${qs}`)
}

/**
 * Strategy-scoped slice of the audit feed, newest first. Backed by
 * `GET /strategies/{id}/decisions`.
 */
export function fetchStrategyDecisions(strategyId: string, limit = 20): Promise<StrategyDecision[]> {
  const qs = new URLSearchParams({ limit: String(limit) })
  return authGet<StrategyDecision[]>(`/api/strategies/${strategyId}/decisions?${qs}`)
}
