/**
 * Live-mode kill switch — lateral menu footer (mock `.killswitch`).
 *
 * API-DEPENDENT — rendered as an inert placeholder for now.
 *
 * Data it will receive once an endpoint exists:
 *   - global trading mode. Expected contract:
 *       GET /api/settings            → { liveMode: boolean }   // false = dry-run
 *     (the orchestrator owns POLYPILOT_LIVE_MODE; no read endpoint yet.)
 *   - the caller's role is already available via `useAuth().session.role` and
 *     decides whether the "go live" action is offered at all.
 *
 * Behaviour to implement when wired:
 *   - dry-run  → yellow pill "DRY-RUN — go live", links to the live-trading
 *                confirm screen (mock 04-confirm-live-trading.html).
 *   - live     → red pill "LIVE — trading enabled", links to a revert confirm.
 *   - poll or subscribe so a mode change made elsewhere is reflected here.
 *
 * No fabricated mode is shown until the read endpoint is real.
 */
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
