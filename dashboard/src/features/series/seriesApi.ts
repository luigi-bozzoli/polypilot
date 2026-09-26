import { authGet } from '../../lib/http'
import { SeriesDetail, SeriesSummary } from './types'

/**
 * Series list with each series' current market and market counts.
 * GET /api/market/series
 */
export function fetchSeriesList(): Promise<SeriesSummary[]> {
  return authGet<SeriesSummary[]>('/api/market/series')
}

/**
 * One series with every market that belongs to it, newest first.
 * GET /api/market/series/:id
 */
export function fetchSeriesDetail(id: string): Promise<SeriesDetail> {
  return authGet<SeriesDetail>(`/api/market/series/${id}`)
}
