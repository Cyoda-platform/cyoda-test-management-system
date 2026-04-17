import { chromium } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../.env.test') });

export default async function globalSetup() {
  const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173';
  const username = process.env.TEST_USERNAME;
  const password = process.env.TEST_PASSWORD;

  if (!username || !password) {
    throw new Error('TEST_USERNAME and TEST_PASSWORD must be set in .env.test');
  }

  const browser = await chromium.launch();
  const page = await browser.newPage();

  await page.goto(baseURL);
  await page.locator('input[placeholder="Enter your username"]').fill(username);
  await page.locator('input[type="password"]').fill(password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL('**/projects**', { timeout: 15_000 });

  await page.context().storageState({ path: 'e2e/.auth/user.json' });
  await browser.close();

  console.log('[global-setup] Auth state saved to e2e/.auth/user.json');
}
