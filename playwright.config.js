import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 45000,
  expect: { timeout: 10000 },
  retries: 0,
  reporter: [
    ["list"],
    ["json", { outputFile: "docs/evidence/playwright-results.json" }],
    ["html", { outputFolder: "playwright-report", open: "never" }],
  ],
  use: {
    baseURL: "http://127.0.0.1:4187",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "desktop-1440",
      use: { browserName: "chromium", viewport: { width: 1440, height: 1000 } },
    },
    {
      name: "mobile-390",
      use: { browserName: "chromium", viewport: { width: 390, height: 844 } },
    },
    {
      name: "mobile-360",
      use: { browserName: "chromium", viewport: { width: 360, height: 800 } },
    },
  ],
  webServer: {
    command: "node tools/e2e-server.mjs",
    url: "http://127.0.0.1:4187/actuator/health/readiness",
    reuseExistingServer: false,
    timeout: 90000,
  },
});
