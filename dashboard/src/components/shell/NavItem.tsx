import { NavLink } from 'react-router-dom'
import type { NavItemConfig } from '../../features/shell/navItems'

const BASE = 'flex items-center gap-3 rounded-md px-2.5 py-2 text-[12.5px]'

/**
 * One row in the lateral menu. Implemented items are real router links with an
 * accent-tinted active state (mock `.nav-item.active`); not-yet-built items are
 * inert, muted rows.
 */
export function NavItem({ item }: { item: NavItemConfig }) {
  const Icon = item.icon

  return (
    <>
      {item.groupStart && <div className="mx-1.5 my-2 h-px bg-border" />}

      {item.implemented ? (
        <NavLink
          to={item.to}
          className={({ isActive }) =>
            `${BASE} transition-colors ${
              isActive
                ? 'bg-accent-dim text-accent'
                : 'text-text-secondary hover:bg-bg2 hover:text-text-primary'
            }`
          }
        >
          <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
          {item.label}
        </NavLink>
      ) : (
        <span
          aria-disabled="true"
          title="Not available yet"
          className={`${BASE} cursor-not-allowed text-text-muted`}
        >
          <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
          {item.label}
        </span>
      )}
    </>
  )
}
