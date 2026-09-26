import { defineConfig, devices } from '@playwright/test'

// Mirrors vite.config.ts's own `process.env.BASE_PATH ?? '/'` fallback exactly, so a bare
// `npm run test:e2e` (no BASE_PATH set) builds and serves under the same base path this config
// points at — CI sets BASE_PATH explicitly (see .github/workflows/demo.yml) to test the real
// GitHub Pages path (`/<repo>/`); local runs default to '/' and still pass on their own.
const basePath = process.env.BASE_PATH ?? '/'
const PORT = 4173

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${PORT}${basePath}`,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // The build happens as its own step (`npm run test:e2e` = `build:demo && playwright test`) so
  // this suite exercises the exact artifact that ships, not a dev-server approximation.
  webServer: {
    command: `npm run preview:demo -- --port ${PORT} --strictPort`,
    url: `http://localhost:${PORT}${basePath}`,
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
})
