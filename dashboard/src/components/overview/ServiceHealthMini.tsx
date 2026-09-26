import { Card, SectionLabel } from '../market/primitives'
import { Empty, ErrorState, Loading } from '../series/ListStates'
import { useHealthQuery } from '../../features/health/useHealthQuery'
import { toneForStatus } from '../../features/health/statusTone'
import { ServiceStatusDot } from '../health/ServiceStatusDot'

/**
 * Compact 3-cell service-health grid (mock `10-overview.html`), backed by the same
 * `GET /api/health` `HealthPage` uses — a denser render of `ServiceCard`, not a new data source.
 */
export function ServiceHealthMini() {
  const { data, isLoading, isError, error, refetch } = useHealthQuery(true)

  return (
    <Card>
      <SectionLabel className="mb-[11px]">Service health</SectionLabel>

      {isLoading && <Loading label="Contacting orchestrator…" />}

      {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

      {!isLoading && !isError && !data && <Empty message="No health data yet." />}

      {data && (
        <div className="grid grid-cols-3 gap-2.5">
          <MiniCell label="Orchestr." status={data.status} detail={data.metrics?.latencyMs != null ? `${data.metrics.latencyMs} ms` : undefined} />
          <MiniCell
            label="Auth svc"
            status={data['auth-service'].status}
            detail={data['auth-service'].metrics?.latencyMs != null ? `${data['auth-service'].metrics.latencyMs} ms` : undefined}
          />
          <MiniCell
            label="AI agent"
            status={data['ai-agent'].status}
            detail={data['ai-agent'].metrics?.latencyMs != null ? `${data['ai-agent'].metrics.latencyMs} ms` : undefined}
          />
        </div>
      )}
    </Card>
  )
}

function MiniCell({ label, status, detail }: { label: string; status: string | undefined; detail?: string }) {
  const tone = toneForStatus(status)
  return (
    <div className="rounded-lg border border-border bg-bg2 px-3 py-2.5">
      <div className="text-[10px] uppercase tracking-[0.06em] text-text-muted">{label}</div>
      <div className="mt-1.5 flex items-center gap-1.5 text-[12px] text-text-primary">
        <ServiceStatusDot tone={tone} />
        {status ?? '—'}
      </div>
      <div className="mt-[3px] font-mono text-[10px] text-text-muted">{detail ?? '—'}</div>
    </div>
  )
}
