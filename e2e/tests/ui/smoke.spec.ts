import { test, expect } from '@playwright/test';
import { API, env } from '../../src/env';
import { bearer, courierUpdate, createParcel, expectStatus, signup, TEST_PASSWORD } from '../../src/api';
import { admin, courier, dispatcher } from '../../src/actors';

/**
 * A few checks through the real frontend (nginx on port 3000, proxying /api to the gateway).
 * Visual diffs are out of scope for now; these only check that the main paths work in a browser.
 */

test('customerSignsInThroughTheLoginPage', async ({ page, request }) => {
  const customer = await signup(request);

  await page.goto('/');
  await page.getByRole('button', { name: 'Customer', exact: true }).click();
  await page.getByLabel('Email or mobile').fill(customer.user.email);
  await page.getByLabel('Password', { exact: true }).fill(TEST_PASSWORD);
  await page.getByRole('button', { name: /^Sign in to Customer portal/ }).click();

  await expect(page).toHaveURL(/\/customer$/);
  // The dashboard greets by first name
  await expect(page.getByRole('heading', { level: 1 })).toContainText(customer.user.fullName.split(' ')[0]);
});

test('wrongPasswordShowsAnErrorAndStaysOnLogin', async ({ page, request }) => {
  const customer = await signup(request);

  await page.goto('/');
  await page.getByLabel('Email or mobile').fill(customer.user.email);
  await page.getByLabel('Password', { exact: true }).fill('not-the-password-123');
  await page.getByRole('button', { name: /^Sign in to/ }).click();

  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page).not.toHaveURL(/\/customer$/);
});

test('signedOutVisitorIsSentToLoginFromAPortal', async ({ page }) => {
  await page.goto('/dispatch');
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
});

test('publicTrackingPageUpdatesLiveWhenTheCourierMovesTheParcel', async ({ page, request }) => {
  const adminSession = await admin(request);
  const [customer, rider, dispatch] = await Promise.all([
    signup(request),
    courier(request, adminSession),
    dispatcher(request),
  ]);
  const parcel = await createParcel(request, customer);
  const assigned = await request.post(`${API}/dispatch/assignments`, {
    headers: bearer(dispatch),
    data: { parcelId: parcel.id, courierId: rider.courierId },
  });
  await expectStatus(assigned, 201);
  const assignment = await assigned.json();

  await page.goto(`/track/${parcel.trackingNo}`);
  await expect(page.locator('#tracking-number')).toHaveText(parcel.trackingNo);
  await expect(page.getByRole('status')).toHaveText('Booked');
  // The SSE stream goes through nginx and the gateway; this fails if either buffers it
  await expect(page.getByText('Live: updates appear automatically')).toBeVisible();

  await expectStatus(await courierUpdate(request, rider.session, assignment.id, 'PICKED_UP'), 200);
  await expect(page.getByRole('status')).toHaveText('Picked up');
  await expect(page.getByRole('list', { name: 'Delivery progress' }).locator('[aria-current="step"]')).toHaveText(
    'Picked up',
  );
});

test('frontendIsServedWithItsApiProxy', async ({ request }) => {
  const health = await request.get(`${env.frontendUrl}/health`);
  expect(health.status()).toBe(200);
  const zones = await request.get(`${env.frontendUrl}/api/v1/pricing/zones`);
  expect(zones.status()).toBe(200);
});
