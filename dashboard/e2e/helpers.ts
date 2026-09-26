import type { Page } from '@playwright/test'

// Relative (no leading slash) so it resolves against baseURL's own path prefix (`/<repo>/` in
// CI) per WHATWG URL rules — a leading-slash path resolves from the origin instead, ignoring
// baseURL's path segment entirely. Applies to every `page.goto(...)` call in this suite.
export async function loginWithPassword(page: Page): Promise<void> {
  await page.goto('')
  await page.getByRole('button', { name: /admin & demo sign-in/i }).click()
  await page.getByRole('button', { name: /^log in$/i }).click()
  await page.waitForURL('**/overview')
}

export async function loginWithWallet(page: Page): Promise<void> {
  await page.goto('')
  await page.getByRole('button', { name: /^connect wallet$/i }).click()
  await page.getByRole('button', { name: /sign-in with ethereum/i }).click()
  await page.waitForURL('**/overview', { timeout: 10_000 })
}
