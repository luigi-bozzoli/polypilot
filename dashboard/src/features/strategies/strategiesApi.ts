import { authDelete, authGet, authPost, authPut } from '../../lib/http'
import type {
  ConditionFieldsCatalog,
  CreateStrategyRequest,
  StrategyOrderRow,
  StrategyView,
  UpdateStrategyRequest,
} from './types'

/** GET /strategies/condition-fields */
export function fetchConditionFields(): Promise<ConditionFieldsCatalog> {
  return authGet<ConditionFieldsCatalog>('/api/strategies/condition-fields')
}

/** GET /strategies */
export function fetchStrategies(): Promise<StrategyView[]> {
  return authGet<StrategyView[]>('/api/strategies')
}

/** GET /strategies/{id}. 404s if the id is unknown. */
export function fetchStrategy(id: string): Promise<StrategyView> {
  return authGet<StrategyView>(`/api/strategies/${id}`)
}

/**
 * POST /strategies. `enabled` is server-assigned (defaults to `true` on create) — not part of
 * the request; use `setStrategyEnabled` to change it afterwards. 409 on a duplicate name, 422 on
 * a rule-tree/business-rule failure, per the contract.
 */
export function createStrategy(request: CreateStrategyRequest): Promise<StrategyView> {
  return authPost<StrategyView>('/api/strategies', request)
}

/**
 * PUT /strategies/{id}. 404 if unknown, not owned by the caller, or deleted; 409 on a
 * duplicate name, 422 on a rule-tree/business-rule failure, per the contract.
 */
export function updateStrategy(id: string, request: UpdateStrategyRequest): Promise<StrategyView> {
  return authPut<StrategyView>(`/api/strategies/${id}`, request)
}

/**
 * DELETE /strategies/{id}. Soft delete — 404 if unknown, not owned by the caller, or already
 * deleted; 409 if the strategy has order or audit history.
 */
export function deleteStrategy(id: string): Promise<void> {
  return authDelete(`/api/strategies/${id}`)
}

/**
 * PUT /strategies/{id}/enabled. Flips the strategy's schedulable state; the orchestrator syncs
 * its scheduler to match in the same call. 404 if unknown, not owned by the caller, or deleted.
 */
export function setStrategyEnabled(id: string, enabled: boolean): Promise<StrategyView> {
  return authPut<StrategyView>(`/api/strategies/${id}/enabled`, { enabled })
}

/**
 * GET /strategies/{id}/orders?limit=20. Strategy-scoped recent orders, newest first. An
 * unknown/unowned id yields an empty array, not a 404 — same as `fetchStrategy`'s sibling
 * `/decisions` endpoint.
 */
export function fetchStrategyOrders(id: string, limit = 20): Promise<StrategyOrderRow[]> {
  return authGet<StrategyOrderRow[]>(`/api/strategies/${id}/orders?limit=${limit}`)
}
