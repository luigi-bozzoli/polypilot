import { useCallback, useState } from 'react'

/**
 * Toggle + persistence for auto-probing on the Service health page.
 *
 * When on, `useHealthQuery` / `useInfrastructureQuery` poll (and refetch on
 * window focus / reconnect) as before; when off, every automatic request stops
 * until the user re-enables it or hits Refresh. The choice is a lasting
 * preference, so it's mirrored to localStorage in the defensive style of
 * `features/shell/menuStorage.ts` — storage access is guarded so private mode /
 * disabled storage degrades to the in-memory default.
 */
const STORAGE_KEY = 'serviceHealth.autoProbeEnabled'

/** Read the persisted preference. Defaults to enabled (auto-probing on). */
export function loadAutoProbeEnabled(): boolean {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw === 'true') return true
    if (raw === 'false') return false
  } catch {
    // Storage unavailable — fall through to the default.
  }
  return true
}

export function saveAutoProbeEnabled(enabled: boolean): void {
  try {
    localStorage.setItem(STORAGE_KEY, enabled ? 'true' : 'false')
  } catch {
    // Nothing we can do; state still lives in memory for this session.
  }
}

export function useAutoProbe(): { enabled: boolean; toggle: () => void } {
  const [enabled, setEnabled] = useState(loadAutoProbeEnabled)

  const toggle = useCallback(() => {
    setEnabled((prev) => {
      const next = !prev
      saveAutoProbeEnabled(next)
      return next
    })
  }, [])

  return { enabled, toggle }
}
