/**
 * Fixed bottom-left pill, mounted by `start.ts` into its own DOM node outside `#root` (its own
 * tiny React root) — so it survives whatever the app shell renders and adds zero code to any
 * real-build component tree. Small by design ("not a bar, so no layout shift").
 */
export function DemoBanner() {
  const repoUrl = import.meta.env.VITE_REPO_URL

  return (
    <div
      style={{
        position: 'fixed',
        left: 12,
        bottom: 12,
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        borderRadius: 8,
        border: '1px solid rgba(255,255,255,0.12)',
        background: '#15181d',
        color: '#7a8394',
        padding: '6px 10px',
        fontFamily: '"IBM Plex Mono", monospace',
        fontSize: 11,
        boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
      }}
    >
      <span style={{ height: 6, width: 6, borderRadius: 999, background: '#00d4a8', flexShrink: 0 }} />
      <span>Demo: simulated data, no backend, no real trading</span>
      {repoUrl && (
        <a
          href={repoUrl}
          target="_blank"
          rel="noreferrer"
          style={{ color: '#00d4a8', textDecoration: 'none', borderLeft: '1px solid rgba(255,255,255,0.12)', paddingLeft: 8 }}
        >
          View source
        </a>
      )}
    </div>
  )
}
