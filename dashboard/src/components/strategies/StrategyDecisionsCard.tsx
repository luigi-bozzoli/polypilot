import { Card, SectionLabel } from '../market/primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useStrategyDecisions } from '../../features/audit/useAuditQueries'
import { decisionBadgeClassName } from '../../features/audit/decisionKind'
import { formatAuditTimestamp } from '../../features/audit/format'

/**
 * "Decisions · audit_logs" (mock `15-strategy-detail.html`). Backed by
 * `GET /api/strategies/{id}/decisions` — the strategy-scoped slice of the audit
 * feed. Newest-first, `detail` rendered verbatim (already formatted server-side).
 */
export function StrategyDecisionsCard({ strategyId }: { strategyId: string }) {
  const { data, isLoading, isError, error, refetch } = useStrategyDecisions(strategyId)

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Decisions · audit_logs</SectionLabel>

      {isLoading && <Loading label="Loading decisions…" />}

      {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

      {data && data.length === 0 && <Empty message="No decisions for this strategy yet." />}

      {data && data.length > 0 && (
        <table className="w-full border-collapse text-[12px]">
          <tbody>
            {data.map((decision) => (
              <tr key={decision.auditLogId}>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-[11px] text-text-muted">
                  {formatAuditTimestamp(decision.occurredAt)}
                </td>
                <td className="border-b border-border px-2.5 py-2.5">
                  <span className={decisionBadgeClassName(decision.kind)}>{decision.kind}</span>
                </td>
                <td className="border-b border-border px-2.5 py-2.5 text-text-secondary">{decision.detail}</td>
                <td
                  className="cursor-not-allowed whitespace-nowrap border-b border-border px-2.5 py-2.5 text-right text-text-muted"
                  title="Audit-detail route not available yet"
                >
                  view →
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  )
}
