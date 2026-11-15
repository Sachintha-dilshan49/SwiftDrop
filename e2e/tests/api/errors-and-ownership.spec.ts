import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { API } from '../../src/env';
import { bearer, createParcel, expectProblem, expectStatus, parcelInput, signup } from '../../src/api';
import { approvedMerchant, merchantOrder } from '../../src/actors';

/** Error contract (RFC 7807, no personal data, no internals) and ownership checks across services. */

// Fake personal data that must never come back in an error
const PHONE = '+94771239876';
const EMAIL = 'leak.check@swiftdrop.test';
const ADDRESS = '99 Leak Check Avenue';

/** Things that must not appear in any error body. */
function expectNoLeaks(body: string) {
  for (const needle of [PHONE, EMAIL, ADDRESS]) expect(body).not.toContain(needle);
  expect(body).not.toMatch(/Exception|\bat lk\.swiftdrop|\bat org\.springframework|SQL|org\.hibernate|stackTrace/i);
}

test.describe('error responses', () => {
  test('validationErrorIsProblemDetailWithoutPersonalData', async ({ request }) => {
    const customer = await signup(request);
    const response = await request.post(`${API}/parcels`, {
      headers: bearer(customer),
      data: parcelInput({
        pickupAddress: ADDRESS,
        recipientPhone: `${PHONE}-not-valid`,
        pickupZone: 'not a zone',
        weightKg: -1,
      }),
    });
    await expectProblem(response, 400);
    expectNoLeaks(await response.text());
  });

  test('malformedJsonIsProblemDetail', async ({ request }) => {
    const customer = await signup(request);
    const response = await request.post(`${API}/parcels`, {
      headers: { ...bearer(customer), 'Content-Type': 'application/json' },
      data: `{"pickupAddress": "${ADDRESS}", `,
    });
    await expectProblem(response, 400);
    expectNoLeaks(await response.text());
  });

  test('unknownParcelIsProblemDetail404', async ({ request }) => {
    const customer = await signup(request);
    await expectProblem(await request.get(`${API}/parcels/${randomUUID()}`, { headers: bearer(customer) }), 404);
  });

  test('signupWithTakenEmailDoesNotEchoTheEmail', async ({ request }) => {
    const customer = await signup(request);
    const response = await request.post(`${API}/auth/signup`, {
      data: { fullName: 'Someone Else', email: customer.user.email, password: 'another-long-password' },
    });
    expect(response.status()).toBe(409);
    expect(await response.text()).not.toContain(customer.user.email);
  });

  test('merchantOrderValidationErrorHasNoPersonalData', async ({ request }) => {
    const merchant = await approvedMerchant(request);
    const response = await request.post(`${API}/merchant-api/orders`, {
      headers: { 'X-API-Key': merchant.apiKey },
      data: merchantOrder({ recipientEmail: 'not-an-email', recipientPhone: PHONE, dropoffAddress: ADDRESS, weightKg: 0 }),
    });
    await expectProblem(response, 400);
    expectNoLeaks(await response.text());
  });
});

test.describe('ownership', () => {
  test('customerCannotSeeAnotherCustomersParcel', async ({ request }) => {
    const owner = await signup(request);
    const other = await signup(request);
    const parcel = await createParcel(request, owner);

    for (const path of [`/parcels/${parcel.id}`, `/parcels/${parcel.id}/receipt`]) {
      const response = await request.get(`${API}${path}`, { headers: bearer(other) });
      expect([403, 404], path).toContain(response.status());
    }
    const list = await request.get(`${API}/parcels`, { headers: bearer(other) });
    await expectStatus(list, 200);
    expect(JSON.stringify(await list.json())).not.toContain(parcel.id);
  });

  test('merchantCannotSeeAnotherMerchantsOrder', async ({ request }) => {
    const [owner, other] = await Promise.all([approvedMerchant(request), approvedMerchant(request)]);
    const created = await request.post(`${API}/merchant-api/orders`, {
      headers: { 'X-API-Key': owner.apiKey },
      data: merchantOrder(),
    });
    await expectStatus(created, 201, 'merchant order');
    const order = (await created.json()) as { id: string };

    const response = await request.get(`${API}/merchants/me/orders/${order.id}`, { headers: bearer(other.session) });
    expect([403, 404]).toContain(response.status());
    const list = await request.get(`${API}/merchants/me/orders`, { headers: bearer(other.session) });
    expect(JSON.stringify(await list.json())).not.toContain(order.id);
  });

  test('pendingMerchantCannotCreateApiKeys', async ({ request }) => {
    const user = await signup(request, 'pending');
    await expectStatus(
      await request.post(`${API}/merchants/register`, {
        headers: bearer(user),
        data: {
          businessName: 'Pending Shop',
          businessRegNo: `PEND-${Date.now()}`,
          contactName: 'Pending Person',
          email: user.user.email,
          phone: '+94770000003',
          address: '5 Waiting Street, Colombo',
        },
      }),
      201,
    );
    const key = await request.post(`${API}/merchants/me/api-keys`, { headers: bearer(user), data: { name: 'x' } });
    expect(key.status()).toBe(403);
  });
});

test.describe('pricing', () => {
  test('parcelFeeComesFromPricingServiceNotTheClient', async ({ request }) => {
    const customer = await signup(request);
    const input = parcelInput({ size: 'MEDIUM', weightKg: 3, speed: 'EXPRESS' });
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
    const quote = (await estimate.json()) as { total: number; currency: string };

    // Extra fee fields in the body must be ignored
    const response = await request.post(`${API}/parcels`, {
      headers: bearer(customer),
      data: { ...input, feeAmount: 1, fee: 1, total: 1 },
    });
    await expectStatus(response, 201);
    const parcel = await response.json();
    expect(Number(parcel.feeAmount)).toBe(Number(quote.total));
    expect(parcel.feeCurrency).toBe('LKR');
    expect(Number(parcel.feeAmount)).toBeGreaterThan(1);
  });

  test('estimateRejectsUnknownZoneWithProblemDetail', async ({ request }) => {
    const response = await request.get(`${API}/pricing/estimate`, {
      params: { pickupZone: 'NOWHERE', dropoffZone: 'KANDY', weightKg: 1, size: 'SMALL', speed: 'STANDARD' },
    });
    expect([400, 404, 422]).toContain(response.status());
    expect(response.headers()['content-type']).toContain('application/problem+json');
  });
});
