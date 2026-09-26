/**
 * Badge tone per audit-log `action`/`kind`. Open-ended on the backend (see
 * `EngineDecisionKind`/`StrategyDecisionKind`) — unrecognized values fall back to `neutral`,
 * matching mock `19-audit-log.html`'s badge coloring (`.badge green|blue|purple|yellow|red`,
 * no modifier class = neutral).
 */
export type DecisionTone = 'green' | 'blue' | 'purple' | 'yellow' | 'red' | 'neutral'

const TONE_BY_KIND: Record<string, DecisionTone> = {
  DRY_RUN_ORDER: 'green',
  ORDER_PLACED: 'green',
  POSITION_OPENED: 'blue',
  STRATEGY_EVALUATED: 'blue',
  SIGNAL_RECEIVED: 'purple',
  ORDER_SKIPPED: 'yellow',
  LIVE_ORDER: 'yellow',
  RISK_BLOCKED: 'red',
}

export function decisionTone(kind: string): DecisionTone {
  return TONE_BY_KIND[kind] ?? 'neutral'
}

const TONE_CLASSNAMES: Record<DecisionTone, string> = {
  green: 'border-accent-border bg-accent-dim text-accent',
  blue: 'border-[rgba(77,158,245,0.3)] bg-blue-dim text-blue',
  purple: 'border-purple-border bg-purple-dim text-purple',
  yellow: 'border-[rgba(240,168,50,0.3)] bg-yellow-dim text-yellow',
  red: 'border-red-border bg-red-dim text-red',
  neutral: 'border-border-mid bg-bg3 text-text-secondary',
}

/** Tailwind classes for a `kind` badge — small pill, mono, uppercase. */
export function decisionBadgeClassName(kind: string): string {
  return `rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${TONE_CLASSNAMES[decisionTone(kind)]}`
}
