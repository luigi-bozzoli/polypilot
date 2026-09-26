import type { TokenSide } from '../strategies/types'

/** One row of `GET /positions` — the caller's open positions. Mirrors `PositionView` (orchestrator). */
export type PositionView = {
  id: string
  marketId: string
  marketQuestion: string
  tokenSide: TokenSide
  size: number
  avgEntryPrice: number
  unrealizedPnl: number
  openedAt: string
}
