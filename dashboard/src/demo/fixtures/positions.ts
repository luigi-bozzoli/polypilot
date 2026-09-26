import type { PositionView } from '../../features/positions/types'
import type { TokenSide } from '../../features/strategies/types'
import { ago, pick, randomFloat, uuid } from '../util'
import { ALL_MARKETS } from './market'

const OPEN_MARKETS = ALL_MARKETS.filter((m) => m.status === 'OPEN')

export const POSITIONS: PositionView[] = OPEN_MARKETS.slice(0, 4).map((market, i) => {
  const avgEntryPrice = Number(randomFloat(0.2, 0.8).toFixed(3))
  const markPrice = market.upPrice ?? avgEntryPrice
  const size = Number(randomFloat(50, 400).toFixed(2))
  return {
    id: uuid('position', i + 1),
    marketId: market.id,
    marketQuestion: market.question,
    tokenSide: pick<TokenSide>(['YES', 'NO']),
    size,
    avgEntryPrice,
    unrealizedPnl: Number(((markPrice - avgEntryPrice) * size).toFixed(2)),
    openedAt: ago((i + 1) * 180),
  }
})
