import { Card, SectionLabel } from '../market/primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useAuditLogs } from '../../features/audit/useAuditQueries'
import { decisionBadgeClassName } from '../../features/audit/decisionKind'
import { formatAuditTimestamp } from '../../features/audit/format'

/**
 * "Recent engine decisions" (mock `10-overview.html`). Backed by `GET /api/audit-logs` — the
 * global audit feed, newest first. Mirrors `StrategyDecisionsCard`'s layout, one column wider
 * (`reasoning` in place of the strategy-scoped `detail`).
 */
export function RecentDecisionsCard() {
  const { data, isLoading, isError, error, refetch } = useAuditLogs(6)

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Recent engine decisions</SectionLabel>

      {isLoading && <Loading label="Loading decisions…" />}

      {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

      {data && data.content.length === 0 && <Empty message="No engine decisions yet." />}

      {data && data.content.length > 0 && (
        <div>
          {data.content.map((entry) => (
            <div key={entry.id} className="flex items-baseline gap-2.5 border-b border-border py-2 text-[12px] last:border-b-0">
              <span className="w-[52px] shrink-0 font-mono text-[10px] text-text-muted">
                {formatAuditTimestamp(entry.createdAt).split(' ').slice(-1)[0]}
              </span>
              <span className={decisionBadgeClassName(entry.action)}>{entry.action}</span>
              <span className="min-w-0 flex-1 truncate text-text-secondary" title={entry.reasoning ?? undefined}>
                {entry.strategyName && <span className="text-text-primary">{entry.strategyName}</span>}
                {entry.strategyName && entry.reasoning ? ' — ' : ''}
                {entry.reasoning ?? ''}
              </span>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
