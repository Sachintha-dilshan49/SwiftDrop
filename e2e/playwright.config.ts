import { defineConfig, devices } from '@playwright/test';
import { env } from './src/env';

const ci = !!process.env.CI;

export default defineConfig({
  testDir: './tests',
  // Async flows (outbox -> RabbitMQ -> consumers) need more than the 30 s default
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: true,
  forbidOnly: ci,
  // One retry in CI only. A test that passes on retry is reported as flaky; it is not ignored.
  retries: ci ? 1 : 0,
  workers: ci ? 2 : 4,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: process.env.PLAYWRIGHT_HTML_OUTPUT_DIR ?? 'playwright-report' }],
    ...(ci
      ? ([
          ['github'],
          ['junit', { outputFile: process.env.PLAYWRIGHT_JUNIT_OUTPUT_FILE ?? 'test-results/junit.xml' }],
        ] as const)
      : []),
  ],
  globalSetup: './src/global-setup.ts',
  use: {
    baseURL: env.gatewayUrl,
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'api', testDir: './tests/api' },
    { name: 'flows', testDir: './tests/flows' },
    {
      // Stops and starts containers, so it runs alone and one test at a time
      name: 'resilience',
      testDir: './tests/resilience',
      fullyParallel: false,
      workers: 1,
      timeout: 240_000,
    },
    {
      name: 'ui',
      testDir: './tests/ui',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: env.frontendUrl,
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
      },
    },
  ],
});
