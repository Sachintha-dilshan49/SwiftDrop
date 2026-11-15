import { test, expect } from '@playwright/test';
import { API } from '../../src/env';
import { bearer, expectStatus } from '../../src/api';
import { approvedMerchant, merchantOrder } from '../../src/actors';
import { signature, stubWebhook, waitForMails, waitForWebhooks } from '../../src/outside';

/** docs/architecture.md §9.2: a merchant's store creates an order with an API key and gets a signed webhook. */

test('merchantOrderCreatesParcelAndSendsSignedWebhook', async ({ request }) => {
  const merchant = await approvedMerchant(request);
  await stubWebhook(merchant.webhookPath);
  const order = merchantOrder();

  const created = await request.post(`${API}/merchant-api/orders`, {
    headers: { 'X-API-Key': merchant.apiKey },
    data: order,
  });
  await expectStatus(created, 201, 'merchant order');
  const body = (await created.json()) as { id: string; parcelId: string; trackingNo: string; feeCurrency: string };
  expect(body.trackingNo).toMatch(/^SD-/);
  expect(body.feeCurrency).toBe('LKR');

  // Trackable publicly like any other parcel
  await expectStatus(await request.get(`${API}/track/${body.trackingNo}`), 200);

  // Listed in the merchant dashboard
  const orders = await request.get(`${API}/merchants/me/orders`, { headers: bearer(merchant.session) });
  await expectStatus(orders, 200);
  expect(JSON.stringify(await orders.json())).toContain(body.trackingNo);

  // The CREATED webhook arrives, signed with the merchant's secret
  const [hook] = await waitForWebhooks(merchant.webhookPath);
  expect(hook.body).toContain(body.trackingNo);
  expect(hook.headers['x-swiftdrop-event-id']).toBeTruthy();
  if (merchant.webhookSecret) {
    expect(hook.headers['x-swiftdrop-signature']).toBe(signature(hook.body, merchant.webhookSecret));
  } else {
    expect(hook.headers['x-swiftdrop-signature']).toMatch(/^sha256=[0-9a-f]{64}$/);
  }

  // The shopper (the recipient) is emailed for merchant orders
  await waitForMails(order.recipientEmail as string);
});

test('sameIdempotencyKeyDoesNotCreateASecondOrder', async ({ request }) => {
  const merchant = await approvedMerchant(request);
  const order = merchantOrder({ idempotencyKey: `e2e-${Date.now()}` });
  const send = () => request.post(`${API}/merchant-api/orders`, { headers: { 'X-API-Key': merchant.apiKey }, data: order });

  const first = await send();
  await expectStatus(first, 201);
  const second = await send();
  expect([200, 201]).toContain(second.status());
  expect((await second.json()).id).toBe((await first.json()).id);

  const orders = await request.get(`${API}/merchants/me/orders`, { headers: bearer(merchant.session) });
  const text = JSON.stringify(await orders.json());
  expect(text.split((await first.json()).trackingNo).length - 1).toBe(1);
});
