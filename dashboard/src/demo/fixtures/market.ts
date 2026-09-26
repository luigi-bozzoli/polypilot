import type { MarketOutcome, MarketStatus, MarketView, SeriesDetail, SeriesSummary } from '../../features/series/types'
import type {
  MarketNewsSummary,
  MarketOhlcHistory,
  MarketOrderRow,
  MarketOrdersResponse,
  MarketPriceHistory,
  MarketPricePoint,
  MarketSentiment,
  OhlcCandle,
  SentimentLabel,
  TimeframeOption,
} from '../../features/market/types'
import type { OrderStatus, TokenSide } from '../../features/strategies/types'
import { ago, fromNow, pick, randomFloat, randomInt, uuid } from '../util'
import { SERIES_IDS, STRATEGY_SEEDS, Ticker } from './ids'

export const TIMEFRAMES: TimeframeOption[] = [
  { code: '15m', label: '15 minutes' },
  { code: '1h', label: '1 hour' },
  { code: '4h', label: '4 hours' },
  { code: '1d', label: '1 day' },
]

const STARTING_PRICE: Record<Ticker, number> = { BTC: 64_000, ETH: 3_200 }
const SYMBOL: Record<Ticker, string> = { BTC: 'BTCUSDT', ETH: 'ETHUSDT' }

type SeriesSeed = {
  id: string
  ticker: Ticker
  recurrence: 'hourly' | 'daily'
  title: string
  statuses: MarketStatus[]
}

const SERIES_SEEDS: SeriesSeed[] = [
  { id: SERIES_IDS.btcHourly, ticker: 'BTC', recurrence: 'hourly', title: 'Bitcoin up or down — hourly', statuses: ['OPEN', 'OPEN', 'CLOSED', 'RESOLVED'] },
  { id: SERIES_IDS.ethDaily, ticker: 'ETH', recurrence: 'daily', title: 'Ethereum up or down — daily', statuses: ['OPEN', 'RESOLVED', 'RESOLVED', 'CANCELLED'] },
  { id: SERIES_IDS.btcDaily, ticker: 'BTC', recurrence: 'daily', title: 'Bitcoin up or down — daily', statuses: ['OPEN', 'OPEN', 'CLOSED'] },
  { id: SERIES_IDS.ethHourly, ticker: 'ETH', recurrence: 'hourly', title: 'Ethereum up or down — hourly', statuses: ['OPEN', 'OPEN', 'CLOSED', 'RESOLVED', 'CANCELLED'] },
]

function buildMarket(seriesIndex: number, marketIndex: number, seed: SeriesSeed, status: MarketStatus, ageMinutes: number): MarketView {
  const price = randomFloat(0.15, 0.85)
  const isSettled = status === 'RESOLVED' || status === 'CANCELLED'
  const outcome: MarketOutcome | null = status === 'RESOLVED' ? pick(['UP', 'DOWN']) : null

  return {
    id: uuid(`market-${seriesIndex}`, marketIndex),
    question: `${seed.ticker} ${seed.recurrence === 'hourly' ? 'up or down this hour' : 'up or down today'} — window ${marketIndex}`,
    category: 'Crypto',
    status,
    upPrice: isSettled && status === 'CANCELLED' ? null : Number(price.toFixed(3)),
    downPrice: isSettled && status === 'CANCELLED' ? null : Number((1 - price).toFixed(3)),
    volume24h: status === 'CANCELLED' ? 0 : Number(randomFloat(5_000, 250_000).toFixed(2)),
    liquidity: status === 'CANCELLED' ? 0 : Number(randomFloat(2_000, 90_000).toFixed(2)),
    resolutionDate: status === 'OPEN' ? fromNow(ageMinutes) : ago(ageMinutes),
    outcome,
    lastSyncedAt: ago(randomInt(0, 5)),
  }
}

const SERIES_DETAILS: SeriesDetail[] = SERIES_SEEDS.map((seed, seriesIndex) => ({
  id: seed.id,
  polymarketId: `pm-series-${seriesIndex + 1}`,
  ticker: seed.ticker,
  slug: `${seed.ticker.toLowerCase()}-${seed.recurrence}`,
  title: seed.title,
  recurrence: seed.recurrence,
  seriesType: 'DEFAULT',
  markets: seed.statuses.map((status, marketIndex) =>
    buildMarket(seriesIndex + 1, marketIndex + 1, seed, status, (marketIndex + 1) * (seed.recurrence === 'hourly' ? 60 : 60 * 24)),
  ),
}))

export const SERIES_LIST: SeriesSummary[] = SERIES_DETAILS.map((detail) => {
  const openMarkets = detail.markets.filter((m) => m.status === 'OPEN')
  const resolvedMarkets = detail.markets.filter((m) => m.status === 'RESOLVED')
  return {
    id: detail.id,
    polymarketId: detail.polymarketId,
    ticker: detail.ticker,
    slug: detail.slug,
    title: detail.title,
    recurrence: detail.recurrence,
    seriesType: detail.seriesType,
    openMarketCount: openMarkets.length,
    resolvedMarketCount: resolvedMarkets.length,
    currentMarket: openMarkets[0] ?? detail.markets[0] ?? null,
  }
})

export function findSeriesDetail(id: string): SeriesDetail | undefined {
  return SERIES_DETAILS.find((s) => s.id === id)
}

export function findMarket(marketId: string): { market: MarketView; series: SeriesDetail } | undefined {
  for (const series of SERIES_DETAILS) {
    const market = series.markets.find((m) => m.id === marketId)
    if (market) return { market, series }
  }
  return undefined
}

export const ALL_MARKETS: MarketView[] = SERIES_DETAILS.flatMap((s) => s.markets)

/* ------------------------------------------------------------------ */
/* Price history                                                      */
/* ------------------------------------------------------------------ */

const WINDOW_MINUTES: Record<string, number> = { '12h': 12 * 60, '24h': 24 * 60, '7d': 7 * 24 * 60 }

export function buildPriceHistory(marketId: string, window: string): MarketPriceHistory {
  const minutes = WINDOW_MINUTES[window] ?? WINDOW_MINUTES['24h']
  const pointCount = 96
  const stepMinutes = minutes / pointCount
  let up = randomFloat(0.3, 0.7)

  const points: MarketPricePoint[] = Array.from({ length: pointCount }, (_, i) => {
    up = Math.min(0.97, Math.max(0.03, up + randomFloat(-0.03, 0.03)))
    const minutesAgo = Math.round(minutes - i * stepMinutes)
    return {
      at: ago(minutesAgo),
      upPrice: Number(up.toFixed(3)),
      downPrice: Number((1 - up).toFixed(3)),
      volume24h: Number(randomFloat(1_000, 50_000).toFixed(2)),
      liquidity: Number(randomFloat(2_000, 90_000).toFixed(2)),
    }
  })

  return { marketId, window, points }
}

/* ------------------------------------------------------------------ */
/* OHLC                                                                */
/* ------------------------------------------------------------------ */

export function buildOhlc(marketId: string, ticker: Ticker, timeframe: string, limit = 200): MarketOhlcHistory {
  const tfMinutes: Record<string, number> = { '15m': 15, '1h': 60, '4h': 240, '1d': 1440 }
  const stepMinutes = tfMinutes[timeframe] ?? 60
  let price = STARTING_PRICE[ticker]

  const candles: OhlcCandle[] = Array.from({ length: limit }, (_, i) => {
    const open = price
    const change = open * randomFloat(-0.01, 0.01)
    const close = Math.max(1, open + change)
    const high = Math.max(open, close) * randomFloat(1.0, 1.005)
    const low = Math.min(open, close) * randomFloat(0.995, 1.0)
    price = close
    const minutesAgo = Math.round((limit - i) * stepMinutes)
    return {
      openTime: ago(minutesAgo),
      open: Number(open.toFixed(2)),
      high: Number(high.toFixed(2)),
      low: Number(low.toFixed(2)),
      close: Number(close.toFixed(2)),
      volume: Number(randomFloat(50, 5_000).toFixed(3)),
      closed: i < limit - 1,
    }
  })

  return { marketId, symbol: SYMBOL[ticker], timeframe, candles }
}

/* ------------------------------------------------------------------ */
/* Orders, news, sentiment — per market                               */
/* ------------------------------------------------------------------ */

const ORDER_STATUSES: OrderStatus[] = ['PENDING', 'OPEN', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED', 'FAILED']
const SENTIMENT_LABELS: SentimentLabel[] = ['BULLISH', 'BEARISH', 'NEUTRAL']

/** The one market that deliberately has no news/sentiment yet — exercises the 204 empty state. */
export const MARKET_WITHOUT_NEWS = SERIES_DETAILS[0].markets[0].id

let orderStatusCursor = 0

export function buildMarketOrders(marketId: string): MarketOrdersResponse {
  const count = randomInt(3, 6)
  const orders: MarketOrderRow[] = Array.from({ length: count }, (_, i) => {
    const strategy = pick(STRATEGY_SEEDS)
    // Cycle through every OrderStatus at least once across the whole demo dataset.
    const status = ORDER_STATUSES[orderStatusCursor % ORDER_STATUSES.length]
    orderStatusCursor += 1
    const sizeRequested = Number(randomFloat(10, 500).toFixed(2))
    const filled = status === 'FILLED' ? sizeRequested : status === 'PARTIALLY_FILLED' ? Number((sizeRequested * randomFloat(0.2, 0.8)).toFixed(2)) : 0
    return {
      id: uuid(`order-${marketId}`, i + 1),
      placedAt: ago(randomInt(1, 600)),
      strategyId: strategy.id,
      strategyName: strategy.name,
      side: pick<TokenSide>(['YES', 'NO']),
      sizeRequested,
      sizeFilled: filled,
      price: Number(randomFloat(0.1, 0.9).toFixed(3)),
      status,
      isDryRun: true,
    }
  })
  return { marketId, orders }
}

export function buildLatestNews(marketId: string): MarketNewsSummary | undefined {
  if (marketId === MARKET_WITHOUT_NEWS) return undefined
  return {
    id: uuid(`news-${marketId}`, 1),
    summary:
      'Coverage this period was mixed: on-chain activity ticked up while macro headlines stayed neutral to slightly cautious.',
    fetchedAt: ago(randomInt(1, 30)),
    expiresAt: ago(-60),
    modelUsed: 'claude-3-5-sonnet',
    sources: [
      { title: 'Market wrap: crypto steady into the close', url: 'https://example.com/news/1', publisher: 'Demo Wire', publishedAt: ago(randomInt(30, 120)) },
      { title: 'Analysts weigh in on short-term direction', url: 'https://example.com/news/2', publisher: 'Demo Daily', publishedAt: ago(randomInt(120, 300)) },
    ],
  }
}

export function buildLatestSentiment(marketId: string): MarketSentiment | undefined {
  if (marketId === MARKET_WITHOUT_NEWS) return undefined
  const label = pick(SENTIMENT_LABELS)
  return {
    id: uuid(`sentiment-${marketId}`, 1),
    label,
    confidence: Number(randomFloat(0.55, 0.95).toFixed(2)),
    score: label === 'BULLISH' ? randomFloat(0.1, 0.9) : label === 'BEARISH' ? randomFloat(-0.9, -0.1) : randomFloat(-0.1, 0.1),
    articleCount: randomInt(2, 12),
    reasoning: 'Aggregated from recent coverage; signal strength reflects article volume and source agreement.',
    modelUsed: 'claude-3-5-sonnet',
    scoredAt: ago(randomInt(1, 30)),
  }
}
