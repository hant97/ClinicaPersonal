import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env['BASE_URL'] || 'http://127.0.0.1:4200';
const chromiumExecutablePath = process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH'];
const chromiumLaunchOptions = chromiumExecutablePath
  ? { executablePath: chromiumExecutablePath }
  : {};

const responsiveTestMatch = /responsive-.*\.spec\.ts/;

/**
 * Playwright E2E Configuration for ClinicaPersonal
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : undefined,
  reporter: [['html', { open: 'never' }], ['list']],
  outputDir: 'test-results',
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: process.env['CI'] ? 'retain-on-failure' : 'off',
  },
  webServer: process.env['PLAYWRIGHT_SKIP_WEBSERVER']
    ? undefined
    : {
        command: `"${process.execPath}" node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1 --port 4200`,
        url: baseURL,
        reuseExistingServer: !process.env['CI'],
        timeout: 120_000,
      },
  projects: [
    {
      name: 'chromium',
      testIgnore: responsiveTestMatch,
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: chromiumLaunchOptions,
      },
    },
    {
      name: 'firefox',
      testIgnore: responsiveTestMatch,
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      testIgnore: responsiveTestMatch,
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'tablet-portrait',
      testMatch: responsiveTestMatch,
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 768, height: 1024 },
        hasTouch: true,
        launchOptions: chromiumLaunchOptions,
      },
    },
    {
      name: 'tablet-landscape',
      testMatch: responsiveTestMatch,
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1024, height: 768 },
        hasTouch: true,
        launchOptions: chromiumLaunchOptions,
      },
    },
    {
      name: 'desktop-standard',
      testMatch: responsiveTestMatch,
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1366, height: 768 },
        launchOptions: chromiumLaunchOptions,
      },
    },
    {
      name: 'desktop-wide',
      testMatch: responsiveTestMatch,
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1920, height: 1080 },
        launchOptions: chromiumLaunchOptions,
      },
    },
  ],
});
