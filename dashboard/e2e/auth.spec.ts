import { test, expect } from '@playwright/test'
import { installGuard, assertClean } from './guard'
import { loginWithPassword, loginWithWallet } from './helpers'

test('password login reaches Overview', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithPassword(page)

  await expect(page.getByRole('heading', { name: /overview/i })).toBeVisible()
  await assertClean(page, guard)
})

test('wallet login reaches Overview', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithWallet(page)

  await expect(page.getByRole('heading', { name: /overview/i })).toBeVisible()
  await assertClean(page, guard)
})

test('logout returns to the login page', async ({ page, baseURL }) => {
  const guard = installGuard(page, baseURL!)
  await loginWithPassword(page)

  await page.getByRole('button', { name: /log out/i }).click()
  await expect(page).toHaveURL(baseURL!)
  await expect(page.getByText('Sign in', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /^connect wallet$/i })).toBeVisible()
  await assertClean(page, guard)
})
