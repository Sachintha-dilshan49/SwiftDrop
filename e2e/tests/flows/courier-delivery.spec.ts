import { test, expect, type APIRequestContext } from '@playwright/test';
import { API, env } from '../../src/env';
import {
  bearer,
  courierUpdate,
  createParcel,
  expectStatus,
  getParcel,
  signup,
  tinyPng,
  type Session,
} from '../../src/api';
import { admin, courier, dispatcher, type Courier } from '../../src/actors';
import { openSse, waitForMails } from '../../src/outside';

/**
 * docs/architecture.md §9.3: a dispatcher assigns a parcel, the courier moves it to DELIVERED with a proof photo,
 * and every change reaches the customer by email and open tracking pages by SSE.
 */

async function assign(request: APIRequestContext, dispatcherSession: Session, parcelId: string, to: Courier) {
  const response = await request.post(`${API}/dispatch/assignments`, {
    headers: bearer(dispatcherSession),
    data: { parcelId, courierId: to.courierId },
  });
  await expectStatus(response, 201, 'assign parcel');
  return (await response.json()) as { id: string; parcelId: string; courierId: string };
}

async function dailyTotals(request: APIRequestContext, dispatcherSession: Session) {
  const today = new Date().toISOString().slice(0, 10);
  const response = await request.get(`${API}/reports/daily`, {
    headers: bearer(dispatcherSession),
    params: { from: today, to: today },
  });
  await expectStatus(response, 200, 'daily report');
  return ((await response.json()) as { totals: { assigned: number; delivered: number; failed: number } }).totals;
}

test('courierDeliversAParcelAndEveryoneIsTold', async ({ request }) => {
  const adminSession = await admin(request);
  const [customer, rider, otherRider, dispatch] = await Promise.all([
    signup(request),
    courier(request, adminSession),
    courier(request, adminSession),
    dispatcher(request),
  ]);
  const parcel = await createParcel(request, customer);
  const before = await dailyTotals(request, dispatch);

  // A tracking page is open for the whole delivery
  const stream = openSse(`${env.gatewayUrl}${API}/track/${parcel.trackingNo}/stream`);
  await stream.ready;

  // The dispatcher sees it waiting and assigns it
  const waiting = await request.get(`${API}/dispatch/unassigned`, { headers: bearer(dispatch) });
  await expectStatus(waiting, 200);
  expect(JSON.stringify(await waiting.json())).toContain(parcel.id);
  const assignment = await assign(request, dispatch, parcel.id, rider);

  // It is on the courier's day list, and not on anyone else's
  const today = await request.get(`${API}/courier/deliveries/today`, { headers: bearer(rider.session) });
  expect(JSON.stringify(await today.json())).toContain(assignment.id);
  const othersToday = await request.get(`${API}/courier/deliveries/today`, { headers: bearer(otherRider.session) });
  expect(JSON.stringify(await othersToday.json())).not.toContain(assignment.id);

  // courierCannotUpdateAnotherCouriersDelivery
  expect((await courierUpdate(request, otherRider.session, assignment.id, 'PICKED_UP')).status()).toBe(403);
  expect(
    (await request.get(`${API}/courier/deliveries/${assignment.id}`, { headers: bearer(otherRider.session) })).status(),
  ).toBe(403);

  // Status machine: no skipping ahead
  expect((await courierUpdate(request, rider.session, assignment.id, 'DELIVERED')).status()).toBe(409);

  // The happy path, with a proof photo on pickup
  await expectStatus(
    await courierUpdate(request, rider.session, assignment.id, 'PICKED_UP', { photo: tinyPng, note: 'Collected' }),
    200,
    'PICKED_UP',
  );
  await expectStatus(await courierUpdate(request, rider.session, assignment.id, 'IN_TRANSIT'), 200, 'IN_TRANSIT');
  await expectStatus(
    await courierUpdate(request, rider.session, assignment.id, 'OUT_FOR_DELIVERY'),
    200,
    'OUT_FOR_DELIVERY',
  );
  // FAILED needs a reason
  expect((await courierUpdate(request, rider.session, assignment.id, 'FAILED')).status()).toBe(400);
  await expectStatus(
    await courierUpdate(request, rider.session, assignment.id, 'DELIVERED', { recipientName: 'E2E Recipient' }),
    200,
    'DELIVERED',
  );
  // Closed deliveries cannot change again
  expect((await courierUpdate(request, rider.session, assignment.id, 'FAILED', { failureReason: 'late' })).status()).toBe(
    409,
  );

  // parcel-service has the full history, with the proof photo on PICKED_UP
  const final = await getParcel(request, customer, parcel.id);
  expect(final.status).toBe('DELIVERED');
  expect(final.history.map((h) => h.status)).toEqual([
    'CREATED',
    'PICKED_UP',
    'IN_TRANSIT',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
  ]);
  const proof = final.history.find((h) => h.status === 'PICKED_UP')?.proofReference;
  expect(proof).toBeTruthy();

  // The tracking page saw every change live, in order. CREATED may also arrive: the outbox relay publishes it
  // asynchronously, so it can land after the page opened.
  await expect
    .poll(
      () =>
        stream.events
          .filter((e) => e.event === 'STATUS_CHANGE')
          .map((e) => JSON.parse(e.data).status)
          .filter((status) => status !== 'CREATED'),
      { timeout: 30_000 },
    )
    .toEqual(['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED']);
  stream.close();

  // One email per status change: CREATED + 4
  await waitForMails(customer.user.email, 5);

  // Reports count the delivery
  await expect
    .poll(async () => (await dailyTotals(request, dispatch)).delivered, { timeout: 30_000 })
    .toBeGreaterThan(before.delivered);
});

test('courierMarksADeliveryFailedWithAReason', async ({ request }) => {
  const adminSession = await admin(request);
  const [customer, rider, dispatch] = await Promise.all([
    signup(request),
    courier(request, adminSession),
    dispatcher(request),
  ]);
  const parcel = await createParcel(request, customer);
  const assignment = await assign(request, dispatch, parcel.id, rider);

  for (const status of ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY']) {
    await expectStatus(await courierUpdate(request, rider.session, assignment.id, status), 200, status);
  }
  await expectStatus(
    await courierUpdate(request, rider.session, assignment.id, 'FAILED', { failureReason: 'Nobody home' }),
    200,
    'FAILED',
  );
  const final = await getParcel(request, customer, parcel.id);
  expect(final.status).toBe('FAILED');

  // A delivered or failed parcel is no longer on the day list as active work
  const today = await request.get(`${API}/courier/deliveries/today`, { headers: bearer(rider.session) });
  const active = ((await today.json()) as { assignmentId: string; status: string }[]).filter(
    (d) => d.assignmentId === assignment.id && d.status !== 'FAILED',
  );
  expect(active).toEqual([]);
});

test('proofPhotoMustBeAnImage', async ({ request }) => {
  const adminSession = await admin(request);
  const [customer, rider, dispatch] = await Promise.all([
    signup(request),
    courier(request, adminSession),
    dispatcher(request),
  ]);
  const parcel = await createParcel(request, customer);
  const assignment = await assign(request, dispatch, parcel.id, rider);

  const response = await courierUpdate(request, rider.session, assignment.id, 'PICKED_UP', {
    photo: Buffer.from('#!/bin/sh\necho not an image\n'),
  });
  expect(response.status()).toBe(400);
  expect((await getParcel(request, customer, parcel.id)).status).toBe('CREATED');
});

test('parcelCannotBeAssignedTwice', async ({ request }) => {
  const adminSession = await admin(request);
  const [customer, first, second, dispatch] = await Promise.all([
    signup(request),
    courier(request, adminSession),
    courier(request, adminSession),
    dispatcher(request),
  ]);
  const parcel = await createParcel(request, customer);
  await assign(request, dispatch, parcel.id, first);
  const again = await request.post(`${API}/dispatch/assignments`, {
    headers: bearer(dispatch),
    data: { parcelId: parcel.id, courierId: second.courierId },
  });
  expect(again.status()).toBe(409);
});
