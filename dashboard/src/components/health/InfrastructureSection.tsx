import { toneForInfraStatus } from '../../features/health/statusTone'
import type { InfraComponent } from '../../features/health/types'
import { useInfrastructureQuery } from '../../features/health/useHealthQuery'
import { PlaceholderStatusCard } from './PlaceholderStatusCard'
import { ServiceStatusDot } from './ServiceStatusDot'

/**
 * Infrastructure status — Postgres, Redis, RabbitMQ on the `polypilot-net`
 * bridge network (mock `21-health.html`, "Infrastructure" section).
 * Backed by GET /api/health/infrastructure — see
 * /contracts/health-infrastructure.md.
 */
const FALLBACK_NAMES = ['Postgres 16', 'Redis 7', 'RabbitMQ 3']

export function InfrastructureSection({ autoProbe }: { autoProbe: boolean }) {
  const { data } = useInfrastructureQuery(autoProbe)

  if (!data) {
    return (
      <div className="grid grid-cols-3 gap-3">
        {FALLBACK_NAMES.map((name) => (
          <PlaceholderStatusCard key={name} label={name} />
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-3 gap-3">
      {data.components.map((component) => (
        <InfraCard key={component.name} component={component} />
      ))}
    </div>
  )
}

function InfraCard({ component }: { component: InfraComponent }) {
  const tone = toneForInfraStatus(component.status)

  return (
    <div className="rounded-[10px] border border-border bg-bg1 px-[15px] py-[13px]">
      <div className="text-[10px] uppercase tracking-wider text-text-muted">{component.name}</div>
      <div className="mt-[7px] flex items-center gap-2 text-[12px] text-text-primary">
        <ServiceStatusDot tone={tone} />
        {component.label ?? component.status}
      </div>
      <div className="mt-1 font-mono text-[10px] text-text-muted">
        :{component.port} · {component.detail}
      </div>
    </div>
  )
}
