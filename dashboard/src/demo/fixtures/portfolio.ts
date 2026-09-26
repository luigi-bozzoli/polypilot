import type { PortfolioSummary } from '../../features/portfolio/types'
import { randomInt } from '../util'
import { POSITIONS } from './positions'

export const PORTFOLIO_SUMMARY: PortfolioSummary = {
  unrealizedPnl: Number(POSITIONS.reduce((sum, p) => sum + p.unrealizedPnl, 0).toFixed(2)),
  realizedPnl7d: Number((POSITIONS.length * randomInt(-50, 120)).toFixed(2)),
  dryOrdersToday: randomInt(4, 22),
  openPositionCount: POSITIONS.length,
}
