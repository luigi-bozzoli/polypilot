import { toneForStatus } from '../../features/health/statusTone'
import type { ServiceMetrics } from '../../features/health/types'
import { ServiceStatusDot } from './ServiceStatusDot'

type ServiceCardProps = {
  name: string
  /** Deployment port from the repo-root — static, not fetched. */
  port: number
  /** The service's own health payload, or undefined while loading. */
  data: { status: string; metrics?: ServiceMetrics;[key: string]: unknown } | undefined
}

/**
 * One service card (mock `.svc`). Status dot, metrics row (uptime / latency /
 * detail) and the raw JSON block all come straight from GET /api/health.
 */
export function ServiceCard({ name, port, data }: ServiceCardProps) {
  const tone = toneForStatus(data?.status)
  const metrics = data?.metrics

  return (
    <div className="rounded-xl border border-border bg-bg1 p-4">
      <div className="mb-2.5 flex items-center gap-2.5">
        <ServiceStatusDot tone={tone} />
        <span className="flex-1 text-[13px] font-medium text-text-primary">{name}</span>
        <span className="font-mono text-[10px] text-text-muted">:{port}</span>
      </div>

      <div className="mb-2.5 flex gap-4 font-mono text-[10px] text-text-muted">
        <span>uptime {metrics?.uptime ?? '—'}</span>
        <span>{metrics?.latencyMs != null ? `${metrics.latencyMs} ms` : '—'}</span>
        <span>{metrics?.detail ?? '—'}</span>
      </div>

      <pre className="overflow-x-auto rounded-md border border-border bg-bg0 p-2.5 font-mono text-[10.5px] leading-relaxed text-text-secondary">
        {data ? JSON.stringify(data, null, 2) : '—'}
      </pre>
    </div>
  )
}
