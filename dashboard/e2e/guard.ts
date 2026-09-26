import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'

export type Guard = {
  /** Uncaught page errors and console.error calls, page-error text included verbatim. */
  errors: string[]
  /** `[demo] UNMOCKED` console lines — a real mocking gap, distinct from any other console error. */
  unmocked: string[]
  /** `/api/*` requests that failed outright (network error, not a 4xx/5xx response). */
  failedApiRequests: string[]
  /** Requests to any origin other than the page's own — aborted before they leave the browser. */
  externalRequests: string[]
}

/**
 * Installs the collectors every scenario asserts against at the end (`assertClean`). Call this
 * before the test's first `page.goto` so nothing is missed.
 *
 * Cross-origin requests are only *observed* (`page.on('request', ...)`), not intercepted via
 * `page.route()`. The plan's spec calls for aborting them, but `page.route()` — even scoped to a
 * URL predicate that never matches same-origin traffic — installs Playwright's CDP `Fetch`
 * domain interception ahead of the Service Worker for every request on the page, which races
 * with MSW's own SW-based interception and intermittently aborts legitimate, same-origin
 * `/api/*` calls with `net::ERR_ABORTED` (verified while building this suite — not a demo-mode
 * bug). Recording without intercepting still fails the suite the moment a real external request
 * is made, which is the property that matters; nothing in demo mode's code paths issues one.
 */
export function installGuard(page: Page, baseURL: string): Guard {
  const guard: Guard = { errors: [], unmocked: [], failedApiRequests: [], externalRequests: [] }
  const pageOrigin = new URL(baseURL).origin

  page.on('pageerror', (err) => guard.errors.push(String(err)))

  page.on('console', (msg) => {
    if (msg.type() !== 'error') return
    const text = msg.text()
    if (text.includes('[demo] UNMOCKED')) {
      guard.unmocked.push(text)
      return
    }
    // Chrome auto-logs this for every non-2xx fetch/XHR response, including expected ones this
    // app handles deliberately (e.g. a 404 for a deleted/unknown strategy — see
    // `strategiesApi.ts#fetchStrategy`'s own doc comment: "404s if the id is unknown"). A real
    // application error surfaces as an actual JS exception (`pageerror`) or a distinct message,
    // not this generic resource-load line, so it isn't useful as a "something broke" signal here.
    if (/^Failed to load resource: the server responded with a status of \d+/.test(text)) return
    guard.errors.push(text)
  })

  page.on('requestfailed', (req) => {
    if (!new URL(req.url()).pathname.includes('/api/')) return
    const errorText = req.failure()?.errorText
    // net::ERR_ABORTED specifically (and only this error text) fires as a *second*, phantom
    // network event for requests the Service Worker actually answered — verified during this
    // suite's development: the request's real `fetch()` promise resolves normally (status
    // logged, no throw) even when this event also fires for the same URL. It is Chromium/CDP
    // double-reporting a SW-intercepted request's superseded "real" network attempt, not an
    // application-visible failure. A genuine mocking gap surfaces as `[demo] UNMOCKED` (501)
    // instead, which `guard.unmocked` already catches independently.
    if (errorText === 'net::ERR_ABORTED') return
    guard.failedApiRequests.push(`${req.method()} ${req.url()} — ${errorText}`)
  })

  page.on('request', (req) => {
    const url = new URL(req.url())
    if (url.protocol === 'data:' || url.protocol === 'blob:') return
    if (url.origin !== pageOrigin) guard.externalRequests.push(req.url())
  })

  return guard
}

/** Every scenario ends with this — the single place that defines "clean". */
export async function assertClean(page: Page, guard: Guard): Promise<void> {
  const unhandled = await page.evaluate(() => window.__demoUnhandled ?? [])
  expect(unhandled, 'window.__demoUnhandled should be empty').toEqual([])
  expect(guard.unmocked, 'no [demo] UNMOCKED console errors').toEqual([])
  expect(guard.failedApiRequests, 'no failed /api/ requests').toEqual([])
  expect(guard.externalRequests, 'no requests left the origin').toEqual([])
  expect(guard.errors, 'no page errors / console errors').toEqual([])
}

declare global {
  interface Window {
    __demoUnhandled?: string[]
  }
}
