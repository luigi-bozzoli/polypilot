import { authGet } from '../../lib/http'
import type { IndicatorCatalogView } from './types'

/**
 * Real orchestrator endpoint (`orchestrator/.../indicator/controller/IndicatorController.java`) —
 * unlike `strategiesApi.ts`, this one is not mocked.
 * GET /api/indicators
 */
export function fetchIndicatorCatalog(): Promise<IndicatorCatalogView> {
  return authGet<IndicatorCatalogView>('/api/indicators')
}
