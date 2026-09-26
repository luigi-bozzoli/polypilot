/**
 * Mirrors the orchestrator's `market.dto.view` payloads
 * (`GET /api/market/series` and `GET /api/market/series/:id`).
 *
 * Numeric money/price fields arrive as JSON numbers (Jackson serializes
 * `BigDecimal` that way); nullable columns arrive as `null`.
 */

export type SeriesType = 'DEFAULT' | 'USER_ADDITION'

export type MarketStatus = 'OPEN' | 'CLOSED' | 'RESOLVED' | 'CANCELLED'

export type MarketOutcome = 'YES' | 'NO' | 'UP' | 'DOWN'

export type MarketView = {
  id: string
  question: string
  category: string | null
  status: MarketStatus
  upPrice: number | null
  downPrice: number | null
  volume24h: number | null
  liquidity: number | null
  resolutionDate: string | null
  outcome: MarketOutcome | null
  lastSyncedAt: string | null
}

export type SeriesSummary = {
  id: string
  polymarketId: string
  ticker: string
  slug: string
  title: string
  recurrence: string
  seriesType: SeriesType | null
  openMarketCount: number
  resolvedMarketCount: number
  currentMarket: MarketView | null
}

export type SeriesDetail = {
  id: string
  polymarketId: string
  ticker: string
  slug: string
  title: string
  recurrence: string
  seriesType: SeriesType | null
  markets: MarketView[]
}
