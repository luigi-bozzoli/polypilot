import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useHealthQuery } from '../features/health/useHealthQuery'
import { useAutoProbe } from '../features/health/useAutoProbe'
import { Loading, ErrorState } from '../components/series/ListStates'
import { ServiceCard } from '../components/health/ServiceCard'
import { HealthControls } from '../components/health/HealthControls'
import { InfrastructureSection } from '../components/health/InfrastructureSection'
import { ChecksSection } from '../components/health/ChecksSection'

const SECTION_LABEL = 'mb-2.5 mt-[22px] text-[10px] uppercase tracking-[0.1em] text-text-muted'

/** Post-login `/health` page (mock `21-health.html`). Renders inside AppShell. */
export function HealthPage() {
  const { enabled: autoProbe, toggle } = useAutoProbe()
  const { data, isLoading, isError, error, refetch, dataUpdatedAt } = useHealthQuery(autoProbe)

  const queryClient = useQueryClient()
  const [isManualRefreshing, setIsManualRefreshing] = useState(false)

  // One-off probe layered on top of the interval — always available, whatever
  // the toggle is set to. `refetchQueries` defaults to `cancelRefetch: true`, so
  // an in-flight interval probe is cancelled and its result discarded rather
  // than racing this one. The `['health']` prefix also covers infrastructure and
  // checks, so the button refreshes the whole page.
  const handleRefresh = () => {
    setIsManualRefreshing(true)
    void queryClient
      .refetchQueries({ queryKey: ['health'] })
      .finally(() => setIsManualRefreshing(false))
  }

  const lastUpdated = dataUpdatedAt
    ? `${new Date(dataUpdatedAt).toLocaleTimeString('en-GB', { timeZone: 'UTC', hour12: false })} UTC`
    : '—'

  return (
    <div>
      <div className="mb-[18px] flex items-start justify-between gap-3">
        <div>
          <h1 className="text-[20px] font-medium text-text-primary">Service health</h1>
          <p className="mt-[3px] text-[12px] text-text-muted">
            GET /api/health — orchestrator fans out to both Python services ·{' '}
            {autoProbe ? 'polls every 5 s' : 'auto-refresh paused'} · last {lastUpdated}
          </p>
        </div>
        <HealthControls
          autoProbe={autoProbe}
          onToggleAutoProbe={toggle}
          onRefresh={handleRefresh}
          isRefreshing={isManualRefreshing}
        />
      </div>

      {isLoading && <Loading label="Contacting orchestrator…" />}

      {isError && <ErrorState message={(error as Error).message} onRetry={() => void refetch()} />}

      {data && (
        <>
          <div className={SECTION_LABEL}>Services</div>
          <div className="grid grid-cols-3 gap-3">
            <ServiceCard
              name="orchestrator"
              port={8080}
              data={{
                service: data.service,
                status: data.status,
                metrics: data.metrics,
                db: data.db,
                scheduler: data.scheduler,
              }}
            />
            <ServiceCard name="auth-service" port={8001} data={data['auth-service']} />
            <ServiceCard name="ai-agent" port={8002} data={data['ai-agent']} />
          </div>

          <div className={SECTION_LABEL}>Infrastructure · polypilot-net</div>
          <InfrastructureSection autoProbe={autoProbe} />

          <div className={SECTION_LABEL}>Checks</div>
          <ChecksSection />
        </>
      )}
    </div>
  )
}
