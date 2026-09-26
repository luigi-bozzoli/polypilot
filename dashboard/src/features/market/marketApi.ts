import { authGet, authGetOptional } from '../../lib/http'
import {
  MarketNewsSummary,
  MarketOhlcHistory,
  MarketOrdersResponse,
  MarketPriceHistory,
  MarketSentiment,
  TimeframeOption,
} from './types'

/**
 * Probability / price history for one market, oldest-first.
 * GET /api/market/:marketId/price-history?window=12h
 *
 * `window` is a look-back hint (`12h`, `24h`, `7d`); the server may clamp it and
 * echoes the effective value back on `MarketPriceHistory.window`. Contract:
 * `contracts/market-price-history.md`.
 */
export function fetchMarketPriceHistory(
  marketId: string,
  window: string,
): Promise<MarketPriceHistory> {
  const qs = new URLSearchParams({ window })
  return authGet<MarketPriceHistory>(`/api/market/${marketId}/price-history?${qs}`)
}

/**
 * OHLC candles for one market's underlying asset, oldest-first.
 * GET /api/market/:marketId/ohlc?timeframe=1h&limit=200
 *
 * `timeframe`/`limit` are optional server-side defaults (see the contract) — omit
 * either to let the backend pick. Contract: `contracts/market-ohlc.md`.
 */
export function fetchMarketOhlc(
  marketId: string,
  timeframe?: string,
  limit?: number,
): Promise<MarketOhlcHistory> {
  const qs = new URLSearchParams()
  if (timeframe) qs.set('timeframe', timeframe)
  if (limit) qs.set('limit', String(limit))
  const suffix = qs.toString() ? `?${qs}` : ''
  return authGet<MarketOhlcHistory>(`/api/market/${marketId}/ohlc${suffix}`)
}

/**
 * Enabled candle intervals, in display order — feeds the OHLC chart's timeframe
 * selector. GET /api/reference/timeframes. Contract: `contracts/market-ohlc.md`.
 */
export function fetchTimeframes(): Promise<TimeframeOption[]> {
  return authGet<TimeframeOption[]>('/api/reference/timeframes')
}

/**
 * Market-scoped recent orders, newest first.
 * GET /api/market/:marketId/orders?limit=20
 *
 * Contract: `contracts/market-recent-orders.md`.
 */
export function fetchMarketOrders(marketId: string, limit = 20): Promise<MarketOrdersResponse> {
  return authGet<MarketOrdersResponse>(`/api/market/${marketId}/orders?limit=${limit}`)
}

/**
 * Latest non-expired news summary for one market, or `undefined` (204 → no
 * row yet). Contract: `contracts/market-news-summary.md`.
 */
export function fetchLatestNews(marketId: string): Promise<MarketNewsSummary | undefined> {
  return authGetOptional<MarketNewsSummary>(`/api/market/${marketId}/news/latest`)
}

/**
 * Latest sentiment score for one market, or `undefined` (204 → no row yet).
 * Contract: `contracts/market-sentiment.md`.
 */
export function fetchLatestSentiment(marketId: string): Promise<MarketSentiment | undefined> {
  return authGetOptional<MarketSentiment>(`/api/market/${marketId}/sentiment/latest`)
}
