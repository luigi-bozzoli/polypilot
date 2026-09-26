import { authGet } from '../../lib/http'
import type { PositionView } from './types'

/** GET /positions?limit=n — the caller's open positions, newest-opened first. */
export function fetchOpenPositions(limit = 20): Promise<PositionView[]> {
  return authGet<PositionView[]>(`/api/positions?limit=${limit}`)
}
