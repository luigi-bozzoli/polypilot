import { Outlet } from 'react-router-dom'
import { MenuProvider } from '../../features/shell/MenuContext'
import { SideNav } from './SideNav'
import { Topbar } from './Topbar'

/**
 * Page shell for every authenticated screen. Used as a React Router layout
 * route in `App.tsx` — the matched child page renders through <Outlet />.
 *
 * Layout: [ SideNav | ( Topbar / main ) ] in a flex row. The menu pushes the
 * content column rather than overlaying it (see SideNav). `main` carries the
 * mock's content padding and max width; pages provide only their own body.
 *
 * Note: `SeriesListPage` predates this shell and still renders its own
 * full-height wrapper and page header, so it doubles up on padding for now.
 * Trim that when the page is rebuilt against the mocks (`HealthPage` has
 * already been redone this way, see `pages/HealthPage.tsx`).
 */
export function AppShell() {
  return (
    <MenuProvider>
      <div className="flex min-h-screen bg-bg0">
        <SideNav />
        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar />
          <main className="mx-auto w-full max-w-[1160px] flex-1 px-7 py-6">
            <Outlet />
          </main>
        </div>
      </div>
    </MenuProvider>
  )
}
