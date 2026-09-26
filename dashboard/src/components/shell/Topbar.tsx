import { useLocation, useNavigate } from 'react-router-dom'
import { LogOut, PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { useMenu } from '../../features/shell/MenuContext'
import { NAV_ITEMS } from '../../features/shell/navItems'
import { useAuth } from '../../features/auth/AuthContext'
import { NotificationsButton } from './NotificationsButton'

/** Current page label, derived from the nav config (mock `.crumb`). */
function usePageTitle(): string {
  const { pathname } = useLocation()
  const match = NAV_ITEMS.find(
    (item) => pathname === item.to || pathname.startsWith(`${item.to}/`),
  )
  return match?.label ?? ''
}

/**
 * Top bar (mock `.topbar`): hamburger toggle, current-page label, notifications
 * (API skeleton), the signed-in user, and a log-out control.
 */
export function Topbar() {
  const { isOpen, toggle } = useMenu()
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const title = usePageTitle()

  const handleLogout = async () => {
    await logout()
    navigate('/', { replace: true })
  }

  return (
    <header className="sticky top-0 z-10 flex h-[52px] shrink-0 items-center gap-3.5 border-b border-border bg-bg0 px-6">
      <button
        type="button"
        onClick={toggle}
        aria-label={isOpen ? 'Collapse menu' : 'Expand menu'}
        aria-expanded={isOpen}
        className="flex h-[30px] w-[30px] items-center justify-center rounded-md border border-border bg-bg2 text-text-secondary hover:text-text-primary"
      >
        {isOpen ? (
          <PanelLeftClose className="h-4 w-4" aria-hidden="true" />
        ) : (
          <PanelLeftOpen className="h-4 w-4" aria-hidden="true" />
        )}
      </button>

      {title && <span className="text-[12px] font-medium text-text-primary">{title}</span>}

      <div className="ml-auto flex items-center gap-2.5">
        <NotificationsButton />

        {session && (
          <span className="flex items-center gap-2 rounded-md border border-border bg-bg2 px-2.5 py-1.5 text-[12px] text-text-secondary">
            <span
              className="h-5 w-5 shrink-0 rounded-full bg-gradient-to-br from-accent to-[#4d9ef5]"
              aria-hidden="true"
            />
            <span className="max-w-[180px] truncate">{session.email}</span>
          </span>
        )}

        <button
          type="button"
          onClick={handleLogout}
          aria-label="Log out"
          className="flex h-[30px] w-[30px] items-center justify-center rounded-md border border-border bg-bg2 text-text-secondary hover:text-text-primary"
        >
          <LogOut className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>
    </header>
  )
}
