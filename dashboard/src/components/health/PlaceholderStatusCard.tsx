/**
 * Muted, non-interactive status card (mock `.ic`) for sections that have no
 * backend data source yet. Shared by `InfrastructureSection` and
 * `ChecksSection` — each documents its own missing contract; this component
 * only renders the shared visual shape.
 */
export function PlaceholderStatusCard({ label, mono = false }: { label: string; mono?: boolean }) {
  return (
    <div className="rounded-[10px] border border-border bg-bg1 px-[15px] py-[13px]">
      <div
        className={`text-[10px] uppercase tracking-wider text-text-muted ${mono ? 'font-mono' : ''}`}
      >
        {label}
      </div>
      <div className="mt-[7px] flex items-center gap-2 text-[12px] text-text-muted">
        <span className="h-[9px] w-[9px] shrink-0 rounded-full bg-text-muted" aria-hidden="true" />
        unavailable
      </div>
      <div className="mt-1 font-mono text-[10px] text-text-muted">no data source yet</div>
    </div>
  )
}
