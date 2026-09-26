export function KillSwitchPill() {
  return (
    <div
      className="flex items-center gap-2 rounded-lg border border-border bg-bg2 px-2.5 py-2 text-[11px] text-text-muted"
      aria-hidden="true"
    >
      <span className="h-[7px] w-[7px] shrink-0 rounded-full bg-text-muted" />
      Trading mode unavailable
    </div>
  )
}
