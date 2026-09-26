import { StatRow } from '../components/overview/StatRow'
import { RecentDecisionsCard } from '../components/overview/RecentDecisionsCard'
import { ServiceHealthMini } from '../components/overview/ServiceHealthMini'
import { ActiveStrategiesCard } from '../components/overview/ActiveStrategiesCard'
import { OpenPositionsCard } from '../components/overview/OpenPositionsCard'

/**
 * Post-login landing page (mock `10-overview.html`). Each widget owns its own query and its own
 * loading/error/empty state (mirrors `HealthPage`'s per-section convention) rather than gating the
 * whole page behind one spinner, since the widgets are independently sourced.
 *
 * The mock's daily-exposure gauge is intentionally not built: nothing computes a strategy's
 * actual spend against `maxDailyExposure` yet (`RiskGuard` is explicitly out of scope per
 * `docs/open_trades.md`). The mock's "Simulated equity" stat is also omitted — no starting-capital
 * baseline exists in the schema to compute it from. See `StatRow`.
 */
export function OverviewPage() {
  return (
    <div>
      <div className="mb-5">
        <h1 className="text-[20px] font-medium text-text-primary">Overview</h1>
        <p className="mt-0.5 text-[12px] text-text-muted">Simulated portfolio — all figures dry-run</p>
      </div>

      <StatRow />

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-[1.4fr_1fr]">
        <RecentDecisionsCard />
        <ServiceHealthMini />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ActiveStrategiesCard />
        <OpenPositionsCard />
      </div>
    </div>
  )
}
