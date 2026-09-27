import { toneForCheckStatus } from '../../features/health/statusTone'
import type { Check } from '../../features/health/types'
import { useHealthChecksQuery } from '../../features/health/useHealthQuery'
import { PlaceholderStatusCard } from './PlaceholderStatusCard'
import { ServiceStatusDot } from './ServiceStatusDot'

/**
 * Deploy / schema checks (mock `21-health.html`, "Checks" section): actuator
 * health, SQL init status, JPA validate-mode table count. Backed by
 * GET /api/health/checks.
 */
const FALLBACK_NAMES = ['/actuator/health', 'spring.sql.init', 'JPA validate']

export function ChecksSection() {
  const { data } = useHealthChecksQuery()

  if (!data) {
    return (
      <div className="grid grid-cols-3 gap-3">
        {FALLBACK_NAMES.map((label) => (
          <PlaceholderStatusCard key={label} label={label} mono />
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-3 gap-3">
      {data.checks.map((check) => (
        <CheckCard key={check.name} check={check} />
      ))}
    </div>
  )
}

function CheckCard({ check }: { check: Check }) {
  const tone = toneForCheckStatus(check.status)

  return (
    <div className="rounded-[10px] border border-border bg-bg1 px-[15px] py-[13px]">
      <div className="font-mono text-[10px] uppercase tracking-wider text-text-muted">
        {check.name}
      </div>
      <div className="mt-[7px] flex items-center gap-2 text-[12px] text-text-primary">
        <ServiceStatusDot tone={tone} />
        {check.status}
      </div>
      <div className="mt-1 font-mono text-[10px] text-text-muted">{check.detail}</div>
    </div>
  )
}
