import { setupWorker } from 'msw/browser'
import { createRoot } from 'react-dom/client'
import { createElement } from 'react'
import { handlers } from './handlers'
import { DemoBanner } from './DemoBanner'

declare global {
  interface Window {
    /** Every unmocked `/api/*` request seen this session — read by the Playwright crawl (`e2e/`). */
    __demoUnhandled?: string[]
  }
}

/**
 * Boots demo mode: starts the MSW worker so every `/api/*` call is answered in-browser, no
 * backend involved. Awaited by `main.tsx` before the first render so nothing renders (and no
 * query fires) before the worker is intercepting.
 *
 * The literal `console.info('__POLYPILOT_DEMO_BUNDLE__')` below is `scripts/check-bundle.mjs`'s
 * marker — keep it in exactly this module and don't rename/relocate without updating that script.
 */
export async function startDemo(): Promise<void> {
  console.info('__POLYPILOT_DEMO_BUNDLE__')

  window.__demoUnhandled = []

  const worker = setupWorker(...handlers)
  await worker.start({
    serviceWorker: {
      url: `${import.meta.env.BASE_URL}mockServiceWorker.js`,
    },
    onUnhandledRequest: 'bypass', // non-/api/ requests (fonts, assets) pass through untouched
    quiet: true,
  })

  // A hard reload bypasses service-worker control until the next navigation; if that happens,
  // force one guarded reload so the very next request is actually intercepted. The session-flag
  // guard stops a loop if the worker genuinely can't take control (e.g. no HTTPS/localhost).
  if (!navigator.serviceWorker.controller) {
    const flag = 'polypilot.demo.reloaded'
    if (!sessionStorage.getItem(flag)) {
      sessionStorage.setItem(flag, 'true')
      window.location.reload()
      // Never resolves — the reload is about to tear this page down.
      await new Promise(() => {})
    }
  }

  document.title = `${document.title} (Demo)`
  mountDemoBanner()
}

/** Mounted outside #root, in its own React root, so the app shell tree carries no demo code. */
function mountDemoBanner(): void {
  const node = document.createElement('div')
  node.id = 'polypilot-demo-banner'
  document.body.appendChild(node)
  createRoot(node).render(createElement(DemoBanner))
}
