import { authGet } from '../../lib/http'
import type { PortfolioSummary } from './types'

/** GET /portfolio/summary — the Overview stat row's data source. */
export function fetchPortfolioSummary(): Promise<PortfolioSummary> {
  return authGet<PortfolioSummary>('/api/portfolio/summary')
}
