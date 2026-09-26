import { Loader2 } from 'lucide-react'

/**
 * Header controls for the Service health page: an Auto-refresh switch and a
 * one-off Refresh button. The switch is a real track/knob toggle (`role="switch"`)
 * so its on/off nature is obvious at a glance; the Refresh button reuses the
 * inline button style from `market/MarketPriceHistoryChart.tsx`.
 */
type HealthControlsProps = {
  autoProbe: boolean
  onToggleAutoProbe: () => void
  onRefresh: () => void
  isRefreshing: boolean
}

export function HealthControls({
  autoProbe,
  onToggleAutoProbe,
  onRefresh,
  isRefreshing,
}: HealthControlsProps) {
  return (
    <div className="flex shrink-0 items-center gap-3">
      <button
        type="button"
        role="switch"
        aria-checked={autoProbe}
        aria-label="Auto-refresh"
        onClick={onToggleAutoProbe}
        className="group flex items-center gap-1.5"
      >
        <span className="font-mono text-[10px] uppercase tracking-wider text-text-muted transition-colors group-hover:text-text-secondary">
          Auto-refresh
        </span>
        <span
          className={`relative inline-flex h-[16px] w-[28px] shrink-0 items-center rounded-full border transition-colors ${
            autoProbe ? 'border-accent-border bg-accent' : 'border-border-mid bg-bg3'
          }`}
        >
          <span
            className={`inline-block h-[10px] w-[10px] rounded-full transition-transform ${
              autoProbe ? 'translate-x-[14px] bg-bg0' : 'translate-x-[3px] bg-text-secondary'
            }`}
          />
        </span>
      </button>

      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing}
        className="flex items-center gap-1 rounded border border-border bg-bg2 px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-text-muted transition-colors hover:text-text-secondary disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isRefreshing && <Loader2 className="h-3 w-3 animate-spin" aria-hidden="true" />}
        Refresh
      </button>
    </div>
  )
}
