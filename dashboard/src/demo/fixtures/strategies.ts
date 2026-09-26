import type { OrderStatus, RuleNode, StrategyOrderRow, StrategyView, TokenSide } from '../../features/strategies/types'
import { ago, pick, randomFloat, randomInt, uuid } from '../util'
import { STRATEGY_SEEDS } from './ids'
import { ALL_MARKETS, findSeriesDetail } from './market'

function seriesTitle(seriesId: string): string {
  return findSeriesDetail(seriesId)?.title ?? 'Unknown series'
}

const MOMENTUM_TREE: RuleNode = {
  type: 'BOOLEAN',
  operator: 'AND',
  left: { type: 'MARKET_FIELD', field: 'up_price', operator: 'GT', value: 0.55 },
  right: {
    type: 'INDICATOR',
    indicatorKey: 'rsi',
    params: { period: '14' },
    outputField: 'value',
    operator: 'GT',
    value: 55,
  },
}

const MEAN_REVERSION_TREE: RuleNode = {
  type: 'MARKET_FIELD',
  field: 'down_price',
  operator: 'GT',
  value: 0.6,
}

const SENTIMENT_GATE_TREE: RuleNode = {
  type: 'BOOLEAN',
  operator: 'AND',
  left: { type: 'MARKET_FIELD', field: 'sentiment', operator: 'EQ', value: 'BULLISH' },
  right: { type: 'MARKET_FIELD', field: 'sentiment.confidence', operator: 'GT', value: 0.7 },
}

/**
 * The 3 strategies `demo/db.ts` seeds on every load. Ids/names come from `fixtures/ids.ts`'s
 * `STRATEGY_SEEDS` — the same identities market orders/decisions and audit-log entries are
 * attributed to, so a strategy's detail page and its cross-referenced activity line up.
 */
export const SEED_STRATEGIES: StrategyView[] = [
  {
    id: STRATEGY_SEEDS[0].id,
    name: STRATEGY_SEEDS[0].name,
    description: 'Enter YES when up-price and RSI both show momentum.',
    tokenSide: 'YES',
    orderType: 'GTC',
    ruleTree: MOMENTUM_TREE,
    maxBetSize: 100,
    maxDailyExposure: 1000,
    stopLossThreshold: 0.15,
    cronExpression: '0 */30 * * * *',
    seriesId: STRATEGY_SEEDS[0].seriesId,
    dryRun: true,
    enabled: true,
    seriesTitle: seriesTitle(STRATEGY_SEEDS[0].seriesId),
    createdAt: ago(60 * 24 * 21),
    updatedAt: ago(60 * 24 * 2),
    tradeCount: 17,
  },
  {
    id: STRATEGY_SEEDS[1].id,
    name: STRATEGY_SEEDS[1].name,
    description: 'Enter NO when down-price overshoots.',
    tokenSide: 'NO',
    orderType: 'GTC',
    ruleTree: MEAN_REVERSION_TREE,
    maxBetSize: 75,
    maxDailyExposure: 600,
    stopLossThreshold: 0.2,
    cronExpression: '0 0 */6 * * *',
    seriesId: STRATEGY_SEEDS[1].seriesId,
    dryRun: true,
    enabled: false,
    seriesTitle: seriesTitle(STRATEGY_SEEDS[1].seriesId),
    createdAt: ago(60 * 24 * 14),
    updatedAt: ago(60 * 24 * 5),
    tradeCount: 4,
  },
  {
    id: STRATEGY_SEEDS[2].id,
    name: STRATEGY_SEEDS[2].name,
    description: 'Enter YES only when sentiment is confidently bullish.',
    tokenSide: 'YES',
    orderType: 'GTD',
    ruleTree: SENTIMENT_GATE_TREE,
    maxBetSize: 150,
    maxDailyExposure: 1200,
    stopLossThreshold: null,
    cronExpression: '0 0 9 * * *',
    seriesId: STRATEGY_SEEDS[2].seriesId,
    dryRun: true,
    enabled: true,
    seriesTitle: seriesTitle(STRATEGY_SEEDS[2].seriesId),
    createdAt: ago(60 * 24 * 30),
    updatedAt: ago(60 * 24 * 1),
    tradeCount: 9,
  },
]

let strategyCounter = SEED_STRATEGIES.length

export function nextStrategyId(): string {
  strategyCounter += 1
  return uuid('strategy', strategyCounter)
}

const ORDER_STATUSES: OrderStatus[] = ['PENDING', 'OPEN', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED', 'FAILED']

/**
 * Orders for a strategy's `/orders` tab. Only the 3 seeded strategies (checked by the caller via
 * `db.ts#isSeedStrategy`) get fixture history — a strategy created this session has none yet,
 * matching the plan's "empty arrays for new ones".
 */
export function buildStrategyOrders(strategyId: string, limit: number): StrategyOrderRow[] {
  const count = Math.min(limit, randomInt(3, 8))
  return Array.from({ length: count }, (_, i) => {
    const market = pick(ALL_MARKETS)
    const status = ORDER_STATUSES[(strategyId.length + i) % ORDER_STATUSES.length]
    const sizeRequested = Number(randomFloat(10, 500).toFixed(2))
    const filled = status === 'FILLED' ? sizeRequested : status === 'PARTIALLY_FILLED' ? Number((sizeRequested * randomFloat(0.2, 0.8)).toFixed(2)) : 0
    return {
      id: uuid(`strategy-order-${strategyId}`, i + 1),
      placedAt: ago(randomInt(1, 800)),
      marketId: market.id,
      marketQuestion: market.question,
      side: pick<TokenSide>(['YES', 'NO']),
      sizeRequested,
      sizeFilled: filled,
      price: Number(randomFloat(0.1, 0.9).toFixed(3)),
      status,
      isDryRun: true,
    }
  })
}
