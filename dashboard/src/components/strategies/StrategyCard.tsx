import { Link } from 'react-router-dom'
import type { StrategyView } from '../../features/strategies/types'

const DOT_COLOR = (strategy: StrategyView) => {
  if (!strategy.enabled) return 'bg-text-muted'
  return 'bg-accent'
}

/** Whole card links to the Strategy Detail page (mock `15-strategy-detail.html`). */
export function StrategyCard({ strategy }: { strategy: StrategyView }) {
  return (
    <Link
      to={`/strategies/${strategy.id}`}
      className={`block rounded-xl border border-border bg-bg1 p-4 transition-colors hover:border-border-mid hover:bg-bg2 ${
        !strategy.enabled ? 'opacity-70' : ''
      }`}
    >
      <div className="flex items-center gap-2.5">
        <span className={`h-2 w-2 flex-shrink-0 rounded-full ${DOT_COLOR(strategy)}`} />
        <span className="flex-1 text-[14px] font-medium text-text-primary">{strategy.name}</span>
        <span
          className={`rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${
            strategy.enabled ? 'border-accent-border bg-accent-dim text-accent' : 'border-border-mid bg-bg3 text-text-secondary'
          }`}
        >
          {strategy.enabled ? 'enabled' : 'disabled'}
        </span>
        {strategy.dryRun && (
          <span className="rounded border border-[rgba(240,168,50,0.3)] bg-yellow-dim px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-yellow">
            dry-run
          </span>
        )}
      </div>

      {strategy.description && <p className="my-2 text-[12px] leading-[1.55] text-text-secondary">{strategy.description}</p>}

      <div className="mt-3 flex flex-wrap gap-5 border-t border-border pt-3">
        <Field label="Cron" value={strategy.cronExpression} />
        <Field label="Side" value={strategy.tokenSide} />
        <Field label="Type" value={strategy.orderType} />
        <Field label="Max bet" value={`$${strategy.maxBetSize.toFixed(2)}`} />
        <Field label="Daily cap" value={`$${strategy.maxDailyExposure.toFixed(2)}`} />
        <Field label="Stop-loss" value={strategy.stopLossThreshold != null ? strategy.stopLossThreshold.toFixed(2) : '—'} />
      </div>
    </Link>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-[9px] uppercase tracking-wider text-text-muted">{label}</div>
      <div className="mt-1 font-mono text-[12px] text-text-primary">{value}</div>
    </div>
  )
}
