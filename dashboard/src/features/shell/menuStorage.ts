/**
 * Persistence for the lateral menu's open/closed state.
 *
 * The menu is push-content (see {@link ../../components/shell/SideNav}), so the
 * user's choice is a lasting layout preference rather than transient UI state —
 * worth remembering across reloads. Mirrors the defensive style of
 * `features/auth/session.ts`: every localStorage access is guarded so private
 * mode / disabled storage degrades to the in-memory default.
 */
const STORAGE_KEY = 'polypilot.menu'

/** Read the persisted preference. Defaults to open (menu visible). */
export function loadMenuOpen(): boolean {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw === 'open') return true
    if (raw === 'closed') return false
  } catch {
    // Storage unavailable — fall through to the default.
  }
  return true
}

export function saveMenuOpen(open: boolean): void {
  try {
    localStorage.setItem(STORAGE_KEY, open ? 'open' : 'closed')
  } catch {
    // Nothing we can do; state still lives in memory for this session.
  }
}
