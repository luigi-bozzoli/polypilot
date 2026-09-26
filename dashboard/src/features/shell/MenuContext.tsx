import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { loadMenuOpen, saveMenuOpen } from './menuStorage'

/**
 * Shared open/closed state for the lateral menu.
 *
 * Why context rather than local state in AppShell:
 *  - the toggle control lives in `Topbar` and the consumer is its sibling
 *    `SideNav`; `AppShell` is a router layout element and can't receive props
 *    from the pages it renders, so prop-drilling would still route through it;
 *  - the preference is persisted (see menuStorage) and future shell chrome
 *    (a mobile scrim, keyboard shortcuts) will need the same state;
 *  - it matches the existing `AuthContext` pattern in this codebase.
 * The provider is mounted by `AppShell`, so the cost is scoped to authed pages.
 */
type MenuContextValue = {
  isOpen: boolean
  toggle: () => void
  open: () => void
  close: () => void
}

const MenuContext = createContext<MenuContextValue | null>(null)

export function MenuProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState<boolean>(() => loadMenuOpen())

  useEffect(() => {
    saveMenuOpen(isOpen)
  }, [isOpen])

  const toggle = useCallback(() => setIsOpen((v) => !v), [])
  const open = useCallback(() => setIsOpen(true), [])
  const close = useCallback(() => setIsOpen(false), [])

  const value = useMemo<MenuContextValue>(
    () => ({ isOpen, toggle, open, close }),
    [isOpen, toggle, open, close],
  )

  return <MenuContext.Provider value={value}>{children}</MenuContext.Provider>
}

export function useMenu(): MenuContextValue {
  const ctx = useContext(MenuContext)
  if (!ctx) throw new Error('useMenu must be used within a MenuProvider')
  return ctx
}
