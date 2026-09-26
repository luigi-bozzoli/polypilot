/**
 * Body of `GET /portfolio/summary`. Mirrors `PortfolioSummaryView` (orchestrator). Deliberately
 * has no "equity" field — nothing in the schema stores a starting-capital baseline yet.
 */
export type PortfolioSummary = {
  unrealizedPnl: number
  realizedPnl7d: number
  dryOrdersToday: number
  openPositionCount: number
}
