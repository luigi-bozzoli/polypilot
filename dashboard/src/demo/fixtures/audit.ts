import type { AuditLogEntry, AuditLogPage, StrategyDecision } from '../../features/audit/types'
import type { EngineDecision, MarketDecisionsResponse } from '../../features/market/types'
import { ago, pick, uuid } from '../util'
import { ALL_MARKETS } from './market'
import { STRATEGY_SEEDS } from './ids'

const ACTIONS = [
  'STRATEGY_EVALUATED',
  'DRY_RUN_ORDER',
  'ORDER_SKIPPED',
  'POSITION_CLOSED',
  'RISK_BLOCKED',
  'SIGNAL_RECEIVED',
  'RULE_EVALUATED',
] as const

const REASONING_BY_ACTION: Record<(typeof ACTIONS)[number], string> = {
  STRATEGY_EVALUATED: 'Rule tree evaluated against the latest market snapshot.',
  DRY_RUN_ORDER: 'Entry conditions met — simulated order recorded (dry-run, no funds at risk).',
  ORDER_SKIPPED: 'Entry conditions met but max daily exposure would be exceeded.',
  POSITION_CLOSED: 'Stop-loss threshold reached — simulated position closed.',
  RISK_BLOCKED: 'Blocked by risk limits before evaluation completed.',
  SIGNAL_RECEIVED: 'New sentiment signal ingested from ai-agent.',
  RULE_EVALUATED: 'Individual rule-tree node evaluated during strategy run.',
}

type AuditEvent = {
  id: string
  createdAt: string
  action: (typeof ACTIONS)[number]
  strategyId: string
  strategyName: string
  marketId: string
  marketQuestion: string
  orderId: string | null
}

/**
 * One master list of synthetic events, generated once at module load, backing the global audit
 * feed (`GET /audit-logs`) and its market-/strategy-scoped slices — so the same event shows up
 * consistently whichever endpoint surfaces it. 45 entries covers every `ACTIONS` value several
 * times over per the plan's "40+ entries" target.
 */
const EVENTS: AuditEvent[] = Array.from({ length: 45 }, (_, i) => {
  const action = ACTIONS[i % ACTIONS.length]
  const strategy = pick(STRATEGY_SEEDS)
  const market = pick(ALL_MARKETS)
  return {
    id: uuid('audit', i + 1),
    createdAt: ago(i * 37 + 5),
    action,
    strategyId: strategy.id,
    strategyName: strategy.name,
    marketId: market.id,
    marketQuestion: market.question,
    orderId: action === 'DRY_RUN_ORDER' ? uuid('audit-order', i + 1) : null,
  }
}).sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)) // newest first, matching every real endpoint here

export function buildAuditLogPage(size: number, page = 0): AuditLogPage {
  const start = page * size
  const content: AuditLogEntry[] = EVENTS.slice(start, start + size).map((e) => ({
    id: e.id,
    createdAt: e.createdAt,
    action: e.action,
    isDryRun: true,
    strategyId: e.strategyId,
    strategyName: e.strategyName,
    marketId: e.marketId,
    marketQuestion: e.marketQuestion,
    orderId: e.orderId,
    reasoning: REASONING_BY_ACTION[e.action],
    signals: null,
  }))
  return {
    content,
    page,
    size,
    totalElements: EVENTS.length,
    totalPages: Math.ceil(EVENTS.length / size),
  }
}

export function buildMarketDecisions(marketId: string, limit: number): MarketDecisionsResponse {
  const decisions: EngineDecision[] = EVENTS.filter((e) => e.marketId === marketId)
    .slice(0, limit)
    .map((e) => ({
      id: e.id,
      at: e.createdAt,
      kind: e.action,
      detail: REASONING_BY_ACTION[e.action],
      strategyId: e.strategyId,
      strategyName: e.strategyName,
      auditLogId: e.id,
    }))
  return { marketId, decisions }
}

export function buildStrategyDecisions(strategyId: string, limit: number): StrategyDecision[] {
  return EVENTS.filter((e) => e.strategyId === strategyId)
    .slice(0, limit)
    .map((e) => ({
      occurredAt: e.createdAt,
      kind: e.action,
      detail: REASONING_BY_ACTION[e.action],
      auditLogId: e.id,
    }))
}
