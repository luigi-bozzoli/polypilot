import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Pencil, Trash2, Zap } from 'lucide-react'
import { useDeleteStrategy, useSetStrategyEnabled, useStrategyDetail } from '../features/strategies/useStrategiesQueries'
import { formatDate } from '../features/series/format'
import { Empty, ErrorState, Loading } from '../components/series/ListStates'
import { Card, SectionLabel } from '../components/market/primitives'
import { RuleTreeView } from '../components/strategies/RuleTreeView'
import { Toggle } from '../components/strategies/Toggle'
import { StrategyRecentOrdersCard } from '../components/strategies/StrategyRecentOrdersCard'
import { StrategyDecisionsCard } from '../components/strategies/StrategyDecisionsCard'

/**
 * Strategy Detail page (mock `dashboard/mocks/15-strategy-detail.html`). Reached
 * by clicking a `StrategyCard` on the Strategies list — route `/strategies/:id`,
 * rendered inside `AppShell`. All strategy fields come from `GET /strategies/{id}`
 * (`useStrategyDetail`); the "Recent orders" / "Decisions" sections have no
 * backend yet and render as documented skeletons (see their own components).
 */

/** UUID → mock-style short id, e.g. "0d3f8c1a…e71b". Mirrors `MarketDetailPage`'s helper. */
function shortId(id: string): string {
  const compact = id.replace(/-/g, '')
  return compact.length > 12 ? `${compact.slice(0, 8)}…${compact.slice(-4)}` : id
}

function DisabledButton({ label, reason, danger = false }: { label: ReactNode; reason: string; danger?: boolean }) {
  // Target route not built yet — same treatment as unimplemented nav items / MarketDetailPage.
  return (
    <span
      aria-disabled="true"
      title={reason}
      className={`flex cursor-not-allowed items-center gap-1.5 rounded-md border px-3.5 py-2 text-[12px] font-medium ${
        danger ? 'border-red-border bg-red-dim text-red opacity-60' : 'border-border-mid bg-bg3 text-text-muted'
      }`}
    >
      {label}
    </span>
  )
}

export function StrategyDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: strategy, isLoading, isError, error, refetch } = useStrategyDetail(id)
  const deleteStrategy = useDeleteStrategy()
  const setEnabled = useSetStrategyEnabled(id ?? '')
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [enabledError, setEnabledError] = useState<string | null>(null)

  const handleDelete = async () => {
    if (!strategy) return
    if (!window.confirm(`Delete strategy "${strategy.name}"? This cannot be undone.`)) return

    setDeleteError(null)
    try {
      await deleteStrategy.mutateAsync(strategy.id)
      navigate('/strategies')
    } catch (err) {
      setDeleteError((err as Error).message)
    }
  }

  const handleToggleEnabled = async () => {
    if (!strategy) return
    setEnabledError(null)
    try {
      await setEnabled.mutateAsync(!strategy.enabled)
    } catch (err) {
      setEnabledError((err as Error).message)
    }
  }

  return (
    <div>
      <Link to="/strategies" className="text-[12px] text-text-muted hover:text-text-primary">
        ← All strategies
      </Link>

      {isLoading && (
        <div className="mt-6">
          <Loading label="Loading strategy…" />
        </div>
      )}

      {isError && (
        <div className="mt-6">
          <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />
        </div>
      )}

      {!isLoading && !isError && !strategy && (
        <div className="mt-6">
          <Empty message="That strategy could not be found." />
        </div>
      )}

      {strategy && (
        <>
          <div className="mb-2 mt-3.5 flex items-start justify-between gap-5">
            <div className="min-w-0">
              <h1 className="flex flex-wrap items-center gap-2 text-[19px] font-medium leading-tight text-text-primary">
                {strategy.name}
                <span
                  className={`rounded border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${
                    strategy.enabled
                      ? 'border-accent-border bg-accent-dim text-accent'
                      : 'border-border-mid bg-bg3 text-text-secondary'
                  }`}
                >
                  {strategy.enabled ? 'enabled' : 'disabled'}
                </span>
                {strategy.dryRun && (
                  <span className="rounded border border-[rgba(240,168,50,0.3)] bg-yellow-dim px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-yellow">
                    dry-run
                  </span>
                )}
              </h1>
              <div className="mt-1.5 font-mono text-[11px] text-text-muted">
                id {shortId(strategy.id)} · created {formatDate(strategy.createdAt)} · updated {formatDate(strategy.updatedAt)}
              </div>
            </div>
            <div className="flex shrink-0 gap-2">
              <Link
                to={`/strategies/${strategy.id}/edit`}
                className="flex items-center gap-1.5 rounded-md border border-border-mid bg-bg3 px-3.5 py-2 text-[12px] font-medium text-text-secondary hover:bg-bg4 hover:text-text-primary"
              >
                <Pencil className="h-3.5 w-3.5" aria-hidden="true" /> Edit
              </Link>
              <DisabledButton
                label={
                  <>
                    <Zap className="h-3.5 w-3.5" aria-hidden="true" /> Enable live
                  </>
                }
                reason="Live-trading confirmation route not available yet"
                danger
              />
              <button
                type="button"
                onClick={() => void handleDelete()}
                disabled={deleteStrategy.isPending}
                className="flex items-center gap-1.5 rounded-md border border-red-border bg-red-dim px-3.5 py-2 text-[12px] font-medium text-red hover:opacity-88 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {deleteStrategy.isPending ? (
                  'Deleting…'
                ) : (
                  <>
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" /> Delete
                  </>
                )}
              </button>
            </div>
          </div>

          {deleteError && <p className="mb-5 text-[12px] text-red">{deleteError}</p>}
          {enabledError && <p className="mb-5 text-[12px] text-red">{enabledError}</p>}

          {strategy.description && (
            <p className="mb-5 max-w-[640px] text-[12.5px] leading-[1.6] text-text-secondary">{strategy.description}</p>
          )}

          <div className="mb-5 flex flex-wrap items-center gap-x-6 gap-y-2">
            <div className="flex items-center gap-2.5">
              <Toggle
                checked={strategy.enabled}
                disabled={setEnabled.isPending}
                onChange={() => void handleToggleEnabled()}
                aria-label="Strategy enabled"
              />
              <span className="text-[12.5px] text-text-primary">
                {setEnabled.isPending
                  ? 'Updating…'
                  : strategy.enabled
                    ? 'Enabled — evaluated on schedule'
                    : 'Disabled — not evaluated'}
              </span>
            </div>
            <div className="flex items-center gap-2.5">
              <Toggle checked={!strategy.dryRun} aria-label="Live trading" />
              <span className="text-[12.5px] text-text-muted">
                Live trading — {strategy.dryRun ? 'off' : 'on'} (dry_run = {String(strategy.dryRun)})
              </span>
            </div>
          </div>

          <div className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
            <Card>
              <SectionLabel className="mb-3">Rule tree · rule_tree JSONB</SectionLabel>
              {strategy.ruleTree ? (
                <div className="font-mono text-[11.5px] leading-[1.5]">
                  <RuleTreeView node={strategy.ruleTree} />
                </div>
              ) : (
                <p className="text-[12px] text-text-muted">No rule tree.</p>
              )}
              <div className="mt-3.5 border-t border-border pt-3 font-mono text-[11px] leading-[1.8] text-text-muted">
                <span className="text-blue">THEN</span> BUY <span className="text-accent">{strategy.tokenSide}</span> · order_type{' '}
                <span className="text-accent">{strategy.orderType}</span> · size = min(max_bet_size, daily_remaining)
              </div>
            </Card>

            <Card>
              <SectionLabel className="mb-3">Config &amp; risk limits</SectionLabel>
              <Kv k="enabled" v={String(strategy.enabled)} />
              <Kv k="dry_run" v={String(strategy.dryRun)} valueClassName={strategy.dryRun ? 'text-yellow' : undefined} />
              <Kv k="cron_expression" v={strategy.cronExpression} />
              <Kv k="token_side" v={strategy.tokenSide} />
              <Kv k="order_type" v={strategy.orderType} />
              <Kv k="max_bet_size" v={strategy.maxBetSize.toFixed(4)} />
              <Kv k="max_daily_exposure" v={strategy.maxDailyExposure.toFixed(4)} />
              <Kv k="stop_loss_threshold" v={strategy.stopLossThreshold != null ? strategy.stopLossThreshold.toFixed(4) : '—'} last />
            </Card>
          </div>

          <div className="mt-4">
            <StrategyRecentOrdersCard strategyId={strategy.id} />
          </div>

          <div className="mt-4">
            <StrategyDecisionsCard strategyId={strategy.id} />
            <div className="mt-3">
              <DisabledButton label="All decisions for this strategy →" reason="Audit log route not available yet" />
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function Kv({ k, v, valueClassName, last = false }: { k: string; v: string; valueClassName?: string; last?: boolean }) {
  return (
    <div className={`flex justify-between text-[12px] py-2 ${last ? '' : 'border-b border-border'}`}>
      <span className="text-text-muted">{k}</span>
      <span className={`font-mono text-text-primary ${valueClassName ?? ''}`}>{v}</span>
    </div>
  )
}
