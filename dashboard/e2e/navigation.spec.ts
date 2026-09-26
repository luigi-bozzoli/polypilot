import { test, expect } from '@playwright/test'
import { installGuard, assertClean } from './guard'
import { loginWithPassword } from './helpers'

test('health page renders service, infra and checks sections', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithPassword(page)

  await page.goto('health')
  await expect(page.getByRole('heading', { name: /service health/i })).toBeVisible()
  // 3 services + 3 infra components + 3 checks = 9 status dots worth of cards; assert the
  // section labels instead of counting dots, since exact card markup isn't the point here.
  await expect(page.getByText('Infrastructure · polypilot-net')).toBeVisible()
  await expect(page.getByText('Checks', { exact: true })).toBeVisible()

  await assertClean(page, guard)
})

test('series list -> series detail -> market detail renders charts', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithPassword(page)

  await page.goto('series')
  await page.locator('a[href*="/series/"]').first().click()
  await expect(page).toHaveURL(/\/series\/.+/)

  await page.locator('a[href*="/markets/"]').first().click()
  await expect(page).toHaveURL(/\/markets\//)

  // MarketPriceHistoryChart + MarketOhlcChart each render a lightweight-charts instance (which
  // itself renders several stacked <canvas> layers per chart, so canvas *count* isn't a stable
  // signal — the attribution link each chart renders below itself is a 1:1 proxy for "a chart
  // mounted here").
  await expect(page.getByText('Charts by TradingView')).toHaveCount(2, { timeout: 10_000 })
  await expect(page.locator('canvas').first()).toBeVisible()

  await assertClean(page, guard)
})

test('a market with no news/sentiment yet renders without crashing (204 response)', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithPassword(page)

  // The first series' first (OPEN) market is deliberately fixture-seeded with no news/sentiment
  // — see demo/fixtures/market.ts's MARKET_WITHOUT_NEWS — and is reached by clicking straight
  // through from the series list, matching how a user would find it.
  await page.goto('series')
  await page.locator('a[href*="/series/"]').first().click()
  await page.locator('a[href*="/markets/"]').first().click()
  await expect(page).toHaveURL(/\/markets\//)

  // NOTE: this asserts the card *renders* something, not literally "No news summary yet…" (the
  // Empty state the real component code intends for a 204). @tanstack/react-query v5 throws
  // internally whenever a queryFn resolves `undefined` (query-core's query.js: "Query data
  // cannot be undefined") — and `authGetOptional`/`fetchLatestNews`/`fetchLatestSentiment`
  // (src/lib/http.ts, src/features/market/marketApi.ts — real, non-demo code) intentionally
  // resolve `undefined` on a 204. That is a genuine pre-existing bug in the real app, reproduced
  // here by demo mode correctly serving the documented 204 contract; it is not a demo-mode issue,
  // and src/lib/http.ts is not in this plan's list of real files demo mode may edit (rule 4) —
  // see the final report's "hard-to-mock decisions" for the full writeup. Both cards render their
  // (currently error-styled) fallback without an uncaught exception, which is what this asserts.
  await expect(page.getByText(/news summary/i)).toBeVisible()
  await expect(page.getByText(/sentiment/i).first()).toBeVisible()

  await assertClean(page, guard)
})
