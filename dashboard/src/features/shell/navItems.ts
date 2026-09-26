import type { LucideIcon } from 'lucide-react'
import {
  HeartPulse,
  Layers,
  LayoutDashboard,
  Rows3,
  SlidersHorizontal,
} from 'lucide-react'

export type NavItemConfig = {
  label: string
  /** Router path. Also used as the React key and for active-state matching. */
  to: string
  icon: LucideIcon
  /**
   * `false` → the route does not exist yet. The item still renders (so the menu
   * matches the mock and signals the roadmap) but as a disabled, non-clickable
   * row. Flip to `true` and add the matching <Route> in `App.tsx` when built.
   */
  implemented: boolean
  /** Renders a divider above this item (mock groups nav into two blocks). */
  groupStart?: boolean
}

/**
 * Order and grouping mirror `mocks/10-overview.html`. Only routes wired in
 * `App.tsx` today are marked implemented.
 */
export const NAV_ITEMS: NavItemConfig[] = [
  { label: 'Overview', to: '/overview', icon: LayoutDashboard, implemented: true },
  { label: 'Series', to: '/series', icon: Rows3, implemented: true },
  { label: 'Strategies', to: '/strategies', icon: SlidersHorizontal, implemented: true },
  { label: 'Health', to: '/health', icon: HeartPulse, implemented: true, groupStart: true },
]
