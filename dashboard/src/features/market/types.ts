

import type { OrderStatus, TokenSide } from '../strategies/types'

/* ------------------------------------------------------------------ */
/* Probability / price history — feeds the (not-yet-built) chart      */
/* ------------------------------------------------------------------ */

/** One sampled point on a market's history. Emitted oldest-first. */
export type MarketPricePoint = {
  /** Snapshot instant, ISO-8601 UTC. */
  at: string
  /** UP-outcome price at `at`, 0..1 (probability of an UP close). Null = gap. */
  upPrice: number | null
  /** DOWN-outcome price at `at`, 0..1. */
  downPrice: number | null
  /** 24h notional volume in USD at `at`, if captured. */
  volume24h?: number | null
  /** Pool liquidity in USD at `at`, if captured. */
  liquidity?: number | null
}

export type MarketPriceHistory = {
  marketId: string
  /** Effective look-back window the server used, e.g. "12h". */
  window: string
  /** Chronological, ascending by `at`. Empty = no snapshots yet. */
  points: MarketPricePoint[]
}

/* ------------------------------------------------------------------ */
/* OHLC candlestick chart — the market's underlying asset price        */
/* (Binance, via the series' linked ticker — unrelated to marketId)    */
/* ------------------------------------------------------------------ */

/** One candle, oldest-first. Contract: contracts/market-ohlc.md. */
export type OhlcCandle = {
  /** Candle open instant, ISO-8601 UTC. */
  openTime: string
  open: number
  high: number
  low: number
  close: number
  volume: number
  /** false only for the rightmost (still-forming) bar, when included. */
  closed: boolean
}

export type MarketOhlcHistory = {
  marketId: string
  /** Null exactly when the market's series has no linked Ticker asset — the
   *  chart's "unavailable" signal, distinct from an empty `candles` list. */
  symbol: string | null
  /** Effective timeframe used, echoed back regardless of `symbol`. */
  timeframe: string
  /** Chronological, ascending by `openTime`. */
  candles: OhlcCandle[]
}

/** One selectable candle interval. GET /api/reference/timeframes. */
export type TimeframeOption = {
  /** Binance interval code, e.g. "1h" — the value to send as the `timeframe` param. */
  code: string
  label: string
}

/* ------------------------------------------------------------------ */
/* Recent orders on this market                                       */
/* ------------------------------------------------------------------ */

/**
 * One row of the market-scoped recent-orders feed. Mirrors `MarketOrderView` (orchestrator) and
 * `StrategyOrderRow` (`features/strategies/types.ts`) — same real `OrderStatus`/`TokenSide`
 * enums, `strategyName` denormalized in place of `marketQuestion` since the market is already
 * known from the request.
 */
export type MarketOrderRow = {
  id: string
  /** Order creation instant, ISO-8601 UTC. */
  placedAt: string
  strategyId: string
  strategyName: string
  side: TokenSide
  sizeRequested: number
  sizeFilled: number
  price: number
  status: OrderStatus
  /** true = simulated. Every row is `true` today — no live order-placement path exists yet. */
  isDryRun: boolean
}

export type MarketOrdersResponse = {
  marketId: string
  orders: MarketOrderRow[]
}

/* ------------------------------------------------------------------ */
/* Latest sentiment score                                             */
/* ------------------------------------------------------------------ */

export type SentimentLabel = 'BULLISH' | 'BEARISH' | 'NEUTRAL'

export type MarketSentiment = {
  id: string
  label: SentimentLabel
  /** Model confidence, 0..1. */
  confidence: number
  /** Optional signed score, -1..1 (negative = bearish). */
  score?: number | null
  articleCount: number
  reasoning: string
  /** LLM model id, e.g. "claude-3-5-sonnet". */
  modelUsed: string
  /** Scoring instant, ISO-8601 UTC. */
  scoredAt: string
}

/* ------------------------------------------------------------------ */
/* Latest news summary                                                */
/* ------------------------------------------------------------------ */

export type NewsSource = {
  title: string
  url: string
  publisher: string
  /** Publish instant, ISO-8601 UTC. */
  publishedAt: string
}

export type MarketNewsSummary = {
  id: string
  summary: string
  /** When the summary was generated, ISO-8601 UTC. */
  fetchedAt: string
  /** When it is considered stale, ISO-8601 UTC (display only). */
  expiresAt: string
  modelUsed: string
  sources: NewsSource[]
}

/* ------------------------------------------------------------------ */
/* Your position on this market                                       */
/* ------------------------------------------------------------------ */

export type PositionSide = 'YES' | 'NO'

export type MarketPosition = {
  marketId: string
  side: PositionSide
  /** Net contract count held. Always > 0 when present. */
  size: number
  /** Volume-weighted average entry price, 0..1. */
  avgEntryPrice: number
  /** Mark-to-market unrealized P&L in USD. Signed. */
  unrealizedPnl: number
  /** Realized P&L in USD on this market so far. Signed. */
  realizedPnl?: number | null
}

/* ------------------------------------------------------------------ */
/* Engine decisions on this market                                    */
/* ------------------------------------------------------------------ */

/**
 * Event kind. Open-ended on the backend; the card styles known values and
 * falls back to a neutral badge for the rest.
 */
export type EngineDecisionKind =
  | 'DRY_RUN_ORDER'
  | 'LIVE_ORDER'
  | 'ORDER_SKIPPED'
  | 'SIGNAL_RECEIVED'
  | 'RULE_EVALUATED'
  | (string & {})

export type EngineDecision = {
  id: string
  /** Event instant, ISO-8601 UTC. */
  at: string
  kind: EngineDecisionKind
  /** One display-ready line, pre-formatted by the backend. */
  detail: string
  strategyId?: string | null
  strategyName?: string | null
  /** Audit-log row id, for the future audit-detail route. */
  auditLogId: string
}

export type MarketDecisionsResponse = {
  marketId: string
  decisions: EngineDecision[]
}
