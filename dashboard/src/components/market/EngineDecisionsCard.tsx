import { Card, SectionLabel } from './primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useMarketDecisions } from '../../features/audit/useAuditQueries'
import { decisionBadgeClassName } from '../../features/audit/decisionKind'
import { formatAuditTimestamp } from '../../features/audit/format'

/**
 * "Engine decisions on this market" (mock `13-market-detail.html`).
 * Backed by `GET /api/market/{marketId}/decisions`. Newest-first, rendered verbatim
 * (the `detail` line is already formatted server-side).
 */
export function EngineDecisionsCard({ marketId }: { marketId: string }) {
  const { data, isLoading, isError, error, refetch } = useMarketDecisions(marketId)

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Engine decisions on this market</SectionLabel>

      {isLoading && <Loading label="Loading decisions…" />}

      {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

      {data && data.decisions.length === 0 && <Empty message="No engine decisions on this market yet." />}

      {data && data.decisions.length > 0 && (
        <table className="w-full border-collapse text-[12px]">
          <tbody>
            {data.decisions.map((decision) => (
              <tr key={decision.id}>
                <td className="whitespace-nowrap border-b border-border px-2.5 py-2.5 font-mono text-[11px] text-text-muted">
                  {formatAuditTimestamp(decision.at)}
                </td>
                <td className="border-b border-border px-2.5 py-2.5">
                  <span className={decisionBadgeClassName(decision.kind)}>{decision.kind}</span>
                </td>
                <td className="border-b border-border px-2.5 py-2.5 text-text-secondary">
                  {decision.detail}
                  {decision.strategyName && (
                    <span className="block text-[11px] text-text-muted">{decision.strategyName}</span>
                  )}
                </td>
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
