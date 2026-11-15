import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { API, env } from '../../src/env';
import { bearer, createParcel, expectStatus, signup } from '../../src/api';
import { admin, approvedMerchant, merchantOrder } from '../../src/actors';
import { compose, mailsTo, rabbitPublish, stubWebhook, waitForMails, waitForWebhooks, waitHealthy } from '../../src/outside';

/**
 * What happens when parts of the system fail. These stop and start containers, so they run in their own
 * project, one at a time (npm run test:resilience), in the nightly job only.
 */

test('noEventIsLostWhileNotificationServiceIsDown', async ({ request }) => {
  const customer = await signup(request);
  compose('stop', 'notification-service');
  try {
    // Booked while nobody is consuming: events wait in the durable queue
    const parcels = [];
    for (let i = 0; i < 3; i++) parcels.push(await createParcel(request, customer));
    await new Promise((resolve) => setTimeout(resolve, 3_000));
    expect(await mailsTo(customer.user.email)).toHaveLength(0);
  } finally {
    compose('start', 'notification-service');
  }
  await waitHealthy('http://localhost:8086/actuator/health');
  await waitForMails(customer.user.email, 3, 90_000);
});

test('duplicateEventSendsOnlyOneEmail', async ({ request }) => {
  const customer = await signup(request);
  const event = {
    eventId: randomUUID(),
    type: 'ParcelStatusChanged',
    version: 1,
    occurredAt: new Date().toISOString(),
    parcelId: randomUUID(),
    trackingNo: 'SD-ZZZZ-E2E2',
    customerId: customer.user.id,
    merchantId: null,
    courierId: null,
    fromStatus: null,
    toStatus: 'CREATED',
    note: 'e2e duplicate delivery check',
    failureReason: null,
  };
  // RabbitMQ delivers at least once, so the same event can arrive twice; consumers must skip the copy (rule 7)
  for (let i = 0; i < 2; i++) {
    expect(await rabbitPublish('parcel.events', 'parcel.status.created', event, { message_id: event.eventId })).toBe(
      true,
    );
  }
  await waitForMails(customer.user.email, 1);
  // Give a second copy time to show up if idempotency were broken
  await new Promise((resolve) => setTimeout(resolve, 5_000));
  expect(await mailsTo(customer.user.email)).toHaveLength(1);
});

test('failedWebhookIsRetriedAndRecorded', async ({ request }) => {
  const merchant = await approvedMerchant(request);
  // The merchant's server fails once, then recovers. The first retry is after about 10 s.
  await stubWebhook(merchant.webhookPath, [500]);

  const created = await request.post(`${API}/merchant-api/orders`, {
    headers: { 'X-API-Key': merchant.apiKey },
    data: merchantOrder(),
  });
  await expectStatus(created, 201, 'merchant order');
  const { trackingNo } = await created.json();

  const hooks = await waitForWebhooks(merchant.webhookPath, 2, 120_000);
  expect(hooks.every((hook) => hook.body.includes(trackingNo))).toBe(true);
  // Same event both times, so the merchant can de-duplicate
  expect(new Set(hooks.map((hook) => hook.headers['x-swiftdrop-event-id'])).size).toBe(1);

  // Admins can see both attempts: the failure, then the success
  const deliveries = await request.get(`${API}/admin/webhook-deliveries`, {
    headers: bearer(await admin(request)),
    params: { size: 100 },
  });
  await expectStatus(deliveries, 200);
  const attempts = ((await deliveries.json()) as { content: { webhookUrl: string; attemptNumber: number; status: string }[] })
    .content.filter((d) => d.webhookUrl.endsWith(merchant.webhookPath))
    .sort((a, b) => a.attemptNumber - b.attemptNumber)
    .map((d) => `${d.attemptNumber}:${d.status}`);
  expect(attempts).toEqual(['1:FAILED', '2:SUCCESS']);
});

test('trackingStreamWorksAgainAfterParcelServiceRestarts', async ({ request }) => {
  const customer = await signup(request);
  const parcel = await createParcel(request, customer);
  compose('restart', 'parcel-service');
  await waitHealthy('http://localhost:8083/actuator/health');
  // Through the gateway again, both the snapshot and the stream
  await expect
    .poll(async () => (await request.get(`${API}/track/${parcel.trackingNo}`)).status(), { timeout: 60_000 })
    .toBe(200);
  const response = await fetch(`${env.gatewayUrl}${API}/track/${parcel.trackingNo}/stream`, {
    headers: { Accept: 'text/event-stream' },
    signal: AbortSignal.timeout(10_000),
  });
  expect(response.status).toBe(200);
  const reader = response.body!.getReader();
  const { value } = await reader.read();
  expect(new TextDecoder().decode(value)).toContain('SNAPSHOT');
  await reader.cancel();
});
