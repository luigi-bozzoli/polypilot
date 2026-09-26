import { NAV_ITEMS } from '../../features/shell/navItems'
import { useMenu } from '../../features/shell/MenuContext'
import { NavItem } from './NavItem'
import { KillSwitchPill } from './KillSwitchPill'

/**
 * Lateral (side) navigation menu.
 *
 * Interaction: push-content. The hamburger in `Topbar` toggles `useMenu()`;
 * this <aside> animates its width between 212px and 0 and the sibling content
 * column reflows to fill the space (it is a normal flex child, not an overlay).
 * The inner track keeps a fixed 212px width so labels don't wrap mid-animation.
 *
 * Structure mirrors mock `10-overview.html`: brand block, nav list (two groups
 * split by a divider), kill-switch pill pinned to the footer.
 */
export function SideNav() {
  const { isOpen } = useMenu()

  return (
    <aside
      aria-label="Primary navigation"
      aria-hidden={!isOpen}
      className={`sticky top-0 h-screen shrink-0 overflow-hidden border-r border-border bg-bg1 transition-[width] duration-200 ease-in-out ${
        isOpen ? 'w-[212px]' : 'w-0'
      }`}
    >
      <div className="flex h-full w-[212px] flex-col">
        {/* Brand */}
        <div className="flex items-center gap-2.5 border-b border-border px-[18px] py-4">
          <span className="flex h-[26px] w-[26px] items-center justify-center rounded-[7px] border border-accent-border bg-accent-dim text-[13px] text-accent">
            {'◆'}
          </span>
          <span className="leading-tight">
            <span className="block text-[14px] font-medium text-text-primary">PolyPilot</span>
            {/* Static label. Wire to a real env/version once an app-config endpoint exists. */}
            <span className="block font-mono text-[9px] text-text-muted">dry-run</span>
          </span>
        </div>

        {/* Nav */}
        <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto p-2.5">
          {NAV_ITEMS.map((item) => (
            <NavItem key={item.to} item={item} />
          ))}
        </nav>

        {/* Footer */}
        <div className="border-t border-border p-3">
          <KillSwitchPill />
        </div>
      </div>
    </aside>
  )
}
