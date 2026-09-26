import type { CreateStrategyRequest, StrategyView, UpdateStrategyRequest } from '../features/strategies/types'
import { ago } from './util'
import { SEED_STRATEGIES, nextStrategyId } from './fixtures/strategies'
import { findSeriesDetail } from './fixtures/market'

/**
 * In-memory strategy store — the only stateful demo domain. A `Map` at module scope, seeded once
 * when this module first loads (i.e. once per page load: ES module state doesn't survive a
 * reload, which is exactly the "resets on reload" behavior the plan calls for). No
 * `localStorage`/`sessionStorage` involved.
 */
const strategies = new Map<string, StrategyView>(SEED_STRATEGIES.map((s) => [s.id, s]))

export function listStrategies(): StrategyView[] {
  return [...strategies.values()].sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
}

export function getStrategy(id: string): StrategyView | undefined {
  return strategies.get(id)
}

export function createStrategy(request: CreateStrategyRequest): StrategyView {
  const id = nextStrategyId()
  const now = ago(0)
  const strategy: StrategyView = {
    ...request,
    dryRun: true, // server-enforced default — no live-trading path exists
    id,
    enabled: true, // server-assigned default on create
    seriesTitle: findSeriesDetail(request.seriesId)?.title ?? 'Unknown series',
    createdAt: now,
    updatedAt: now,
    tradeCount: 0,
  }
  strategies.set(id, strategy)
  return strategy
}

export function updateStrategy(id: string, request: UpdateStrategyRequest): StrategyView | undefined {
  const existing = strategies.get(id)
  if (!existing) return undefined
  const updated: StrategyView = {
    ...existing,
    ...request,
    dryRun: true,
    seriesTitle: findSeriesDetail(request.seriesId)?.title ?? existing.seriesTitle,
    updatedAt: ago(0),
  }
  strategies.set(id, updated)
  return updated
}

export function deleteStrategy(id: string): boolean {
  return strategies.delete(id)
}

export function setStrategyEnabled(id: string, enabled: boolean): StrategyView | undefined {
  const existing = strategies.get(id)
  if (!existing) return undefined
  const updated: StrategyView = { ...existing, enabled, updatedAt: ago(0) }
  strategies.set(id, updated)
  return updated
}

/** True for one of the 3 seeded strategies — used to decide whether `/orders`/`/decisions` for a
 *  strategy should return fixture activity or an empty list (a strategy created in this session
 *  has no history yet). */
export function isSeedStrategy(id: string): boolean {
  return SEED_STRATEGIES.some((s) => s.id === id)
}
