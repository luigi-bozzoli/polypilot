import { test, expect } from '@playwright/test'
import { installGuard, assertClean } from './guard'
import { loginWithPassword } from './helpers'

// `*=` ("contains"), not `^=` ("starts with"): under a non-root BASE_PATH (e.g. `/polypilot/`)
// every href is prefixed with the base, so a starts-with match against a root-relative pattern
// would never match. The `:not([href*="/new"])` exclusion still works unprefixed since it only
// needs the "/new" tail to be present anywhere in the href.
const strategyLinks = (page: import('@playwright/test').Page) =>
  page.locator('a[href*="/strategies/"]:not([href*="/new"])')

test('strategies: list, create, edit, toggle, delete, and reset on reload', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithPassword(page)

  // Seeded three, always present at the start of every test (each test gets a fresh page/context).
  await page.goto('strategies')
  await expect(strategyLinks(page)).toHaveCount(3)

  // Open a seeded strategy and save it unchanged.
  await strategyLinks(page).first().click()
  await expect(page).toHaveURL(/\/strategies\/.+/)
  await page.getByRole('link', { name: /edit/i }).click()
  await page.getByRole('button', { name: /^next/i }).click()
  await page.getByRole('button', { name: /^next/i }).click()
  await page.getByRole('button', { name: /save changes/i }).click()
  await expect(page).toHaveURL(/\/strategies\/[^/]+$/)

  // Toggle enabled on the strategy detail page.
  const enabledSwitch = page.locator('button[role="switch"][aria-label="Strategy enabled"]')
  const before = await enabledSwitch.getAttribute('aria-checked')
  await enabledSwitch.click()
  await expect(enabledSwitch).toHaveAttribute('aria-checked', before === 'true' ? 'false' : 'true')

  // Create a new strategy via the builder.
  await page.goto('strategies/new')
  await page.getByPlaceholder('BTC Bullish Momentum').fill('E2E Test Strategy')
  await page.locator('#strategy-series').selectOption({ index: 1 })
  await page.getByRole('button', { name: /^next/i }).click()
  await page.getByRole('button', { name: /^condition$/i }).click()
  await page.getByRole('button', { name: /^next/i }).click()
  await page.getByRole('button', { name: /create strategy/i }).click()
  await expect(page).toHaveURL(/\/strategies$/)
  await expect(strategyLinks(page)).toHaveCount(4)

  // Delete the strategy just created (newest — sorted first by demo/db.ts).
  await strategyLinks(page).first().click()
  await expect(page.getByRole('heading', { name: /E2E Test Strategy/ })).toBeVisible()
  page.once('dialog', (dialog) => void dialog.accept())
  await page.getByRole('button', { name: /^delete$/i }).click()
  await expect(page).toHaveURL(/\/strategies$/)
  await expect(strategyLinks(page)).toHaveCount(3)

  // Reload -> back to exactly the 3 seeded strategies (in-memory store resets on reload).
  await page.reload()
  await expect(strategyLinks(page)).toHaveCount(3)

  await assertClean(page, guard)
})
