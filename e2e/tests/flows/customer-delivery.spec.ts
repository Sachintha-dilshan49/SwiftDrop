import { test, expect } from '@playwright/test';
import { API } from '../../src/env';
import { bearer, createParcel, expectStatus, parcelInput, signup } from '../../src/api';
import { waitForMails } from '../../src/outside';

/** docs/architecture.md §9.1: a customer gets a fee estimate, books a delivery and tracks it. */

test('customerBooksADeliveryAndTracksIt', async ({ request }) => {
  const customer = await signup(request);
  const input = parcelInput({ size: 'MEDIUM', weightKg: 2.25, speed: 'STANDARD' });

  // 1. Public estimate, no login needed
  const estimate = await request.get(`${API}/pricing/estimate`, {
    params: {
      pickupZone: input.pickupZone,
      dropoffZone: input.dropoffZone,
      weightKg: input.weightKg,
      size: input.size,
      speed: input.speed,
    },
  });
  await expectStatus(estimate, 200);
  const quote = await estimate.json();
  expect(quote.currency).toBe('LKR');

  // 2. Book it; the fee must match the estimate
  const parcel = await createParcel(request, customer, input);
  expect(parcel.status).toBe('CREATED');
  expect(parcel.trackingNo).toMatch(/^SD-/);
  expect(Number(parcel.feeAmount)).toBe(Number(quote.total));
  expect(parcel.history.map((h) => h.status)).toEqual(['CREATED']);

  // 3. It is in the customer's own list
  const mine = await request.get(`${API}/parcels`, { headers: bearer(customer) });
  await expectStatus(mine, 200);
  expect(JSON.stringify(await mine.json())).toContain(parcel.trackingNo);

  // 4. Anyone can track it by number, and the public view has no personal data
  const tracking = await request.get(`${API}/track/${parcel.trackingNo}`);
  await expectStatus(tracking, 200);
  const publicView = await tracking.json();
  expect(publicView).toMatchObject({ trackingNo: parcel.trackingNo, status: 'CREATED' });
  const publicText = JSON.stringify(publicView);
  for (const personal of [input.recipientName, input.recipientPhone, input.pickupAddress, input.dropoffAddress]) {
    expect(publicText).not.toContain(personal);
  }

  // 5. Receipt PDF
  const receipt = await request.get(`${API}/parcels/${parcel.id}/receipt`, { headers: bearer(customer) });
  await expectStatus(receipt, 200);
  expect(receipt.headers()['content-type']).toContain('application/pdf');
  expect((await receipt.body()).subarray(0, 4).toString()).toBe('%PDF');

  // 6. Booking confirmation email reaches the customer (outbox -> RabbitMQ -> notification-service -> Mailpit)
  const [mail] = await waitForMails(customer.user.email);
  expect(mail.Subject + mail.Snippet).toContain(parcel.trackingNo);
});

test('unknownTrackingNumberIs404AndMalformedIs400', async ({ request }) => {
  // Valid format (SD-XXXX-XXXX, no 0/1/I/O) that is never generated in practice
  expect((await request.get(`${API}/track/SD-ZZZZ-ZZZZ`)).status()).toBe(404);
  expect((await request.get(`${API}/track/SD-NOPE-0000`)).status()).toBe(400);
});
