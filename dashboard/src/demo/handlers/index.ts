import { http, HttpResponse } from 'msw'
import { authHandlers } from './auth'
import { marketHandlers } from './market'
import { indicatorHandlers } from './indicators'
import { portfolioHandlers } from './portfolio'
import { positionsHandlers } from './positions'
import { auditHandlers } from './audit'
import { healthHandlers } from './health'
import { strategiesHandlers } from './strategies'

/**
 * One MSW handler array per API domain, concatenated here. Order matters only for the
 * catch-all below, which MUST stay last: MSW matches handlers in registration order, so any
 * domain handler registered after it would never run.
 */
export const handlers = [
  ...authHandlers,
  ...marketHandlers,
  ...indicatorHandlers,
  ...strategiesHandlers,
  ...portfolioHandlers,
  ...positionsHandlers,
  ...auditHandlers,
  ...healthHandlers,

  // Catch-all: anything not matched above is a mocking gap, not a real network call. Loud and
  // deterministic (501, not a hang or a silent passthrough) so a missing handler fails fast in
  // both manual testing and the Playwright crawl (`e2e/`).
  http.all('*/api/*', ({ request }) => {
    const entry = `${request.method} ${new URL(request.url).pathname}`
    if (typeof window !== 'undefined') {
      window.__demoUnhandled ??= []
      window.__demoUnhandled.push(entry)
    }
    console.error('[demo] UNMOCKED', request.method, request.url)
    return HttpResponse.json({ detail: 'not mocked in demo' }, { status: 501 })
  }),
]
