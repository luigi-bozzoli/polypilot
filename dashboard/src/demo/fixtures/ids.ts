import { uuid } from '../util'

/**
 * Shared identifiers so IDs line up across fixture files (a market referenced by an order also
 * exists in the series list, a strategy referenced by a decision also exists in `db.ts`, etc.) —
 * see the plan's "keep IDs consistent everywhere" rule. Kept in one place instead of derived
 * per-file so nothing drifts.
 */

export const TICKERS = ['BTC', 'ETH'] as const
export type Ticker = (typeof TICKERS)[number]

export const SERIES_IDS = {
  btcHourly: uuid('series', 1),
  btcDaily: uuid('series', 2),
  ethHourly: uuid('series', 3),
  ethDaily: uuid('series', 4),
} as const

/** The 3 strategies seeded into `demo/db.ts` (Phase 6) — referenced here so market/audit/
 *  portfolio fixtures can point at real strategy ids/names without a circular import on `db.ts`. */
export const STRATEGY_SEEDS = [
  { id: uuid('strategy', 1), name: 'BTC hourly momentum', seriesId: SERIES_IDS.btcHourly },
  { id: uuid('strategy', 2), name: 'ETH daily mean reversion', seriesId: SERIES_IDS.ethDaily },
  { id: uuid('strategy', 3), name: 'BTC daily sentiment gate', seriesId: SERIES_IDS.btcDaily },
] as const
