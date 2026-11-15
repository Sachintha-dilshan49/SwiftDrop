import { test, expect } from '@playwright/test';
import { API, demoAccounts } from '../../src/env';
import { bearer, expectStatus, login, signup, TEST_PASSWORD } from '../../src/api';
import { approvedMerchant } from '../../src/actors';

/** What the gateway must and must not let through (docs/architecture.md §6.1 and §8). */

test.describe('internal routes are never exposed', () => {
  for (const path of [
    '/internal/parcels/00000000-0000-0000-0000-000000000000/contacts',
    '/api/v1/internal/parcels/00000000-0000-0000-0000-000000000000/contacts',
    '/internal/users/00000000-0000-0000-0000-000000000000/contact',
    '/.well-known/jwks.json',
  ]) {
    test(`gatewayDoesNotRoute ${path}`, async ({ request }) => {
      const admin = await login(request, demoAccounts.admin);
      for (const headers of [{}, bearer(admin), { ...bearer(admin), 'X-Internal-Token': 'forged' }]) {
        const response = await request.get(path, { headers });
        expect([401, 403, 404], `${path} ${JSON.stringify(Object.keys(headers))}`).toContain(response.status());
      }
    });
  }

  test('actuatorExposesOnlyHealth', async ({ request }) => {
    expect((await request.get('/actuator/health')).status()).toBe(200);
    for (const path of ['/actuator', '/actuator/env', '/actuator/beans', '/actuator/gateway/routes']) {
      expect([401, 404], path).toContain((await request.get(path)).status());
    }
  });
});

test.describe('JWT checks', () => {
  test('missingTokenIsRejected', async ({ request }) => {
    expect((await request.get(`${API}/parcels`)).status()).toBe(401);
  });

  test('tamperedTokenIsRejected', async ({ request }) => {
    const { accessToken } = await signup(request);
    const [header, payload, sig] = accessToken.split('.');
    // Same signature, but the payload now claims ADMIN
    const claims = JSON.parse(Buffer.from(payload, 'base64url').toString());
    claims.roles = ['ADMIN'];
    const forgedPayload = Buffer.from(JSON.stringify(claims)).toString('base64url');
    for (const token of [
      `${header}.${forgedPayload}.${sig}`,
      `${header}.${payload}.${sig.slice(0, -4)}AAAA`,
      // alg "none", no signature
      `${Buffer.from('{"alg":"none","typ":"JWT"}').toString('base64url')}.${forgedPayload}.`,
      'not-a-jwt',
    ]) {
      expect((await request.get(`${API}/admin/users`, { headers: bearer(token) })).status()).toBe(401);
    }
  });

  test('refreshTokenIsRotatedAndOldOneStopsWorking', async ({ request }) => {
    const session = await signup(request);
    const first = await request.post(`${API}/auth/refresh`, { data: { refreshToken: session.refreshToken } });
    await expectStatus(first, 200);
    const rotated = await first.json();
    expect(rotated.refreshToken).not.toBe(session.refreshToken);

    const reused = await request.post(`${API}/auth/refresh`, { data: { refreshToken: session.refreshToken } });
    expect(reused.status()).toBe(401);
  });

  test('logoutRevokesTheRefreshToken', async ({ request }) => {
    const session = await signup(request);
    const again = await login(request, session.user.email, TEST_PASSWORD);
    await expectStatus(await request.post(`${API}/auth/logout`, { data: { refreshToken: again.refreshToken } }), 204);
    expect((await request.post(`${API}/auth/refresh`, { data: { refreshToken: again.refreshToken } })).status()).toBe(
      401,
    );
  });

  test('wrongPasswordIsRejectedWithoutSayingWhichPartWasWrong', async ({ request }) => {
    const session = await signup(request);
    const wrongPassword = await request.post(`${API}/auth/login`, {
      data: { emailOrMobile: session.user.email, password: 'definitely-not-it' },
    });
    const unknownUser = await request.post(`${API}/auth/login`, {
      data: { emailOrMobile: `nobody.${Date.now()}@swiftdrop.test`, password: 'definitely-not-it' },
    });
    expect(wrongPassword.status()).toBe(401);
    expect(unknownUser.status()).toBe(401);
    expect((await wrongPassword.json()).detail).toBe((await unknownUser.json()).detail);
  });
});

test.describe('merchant API keys', () => {
  test('merchantApiNeedsAValidKey', async ({ request }) => {
    const url = `${API}/merchant-api/orders`;
    expect((await request.post(url, { data: {} })).status()).toBe(401);
    expect((await request.post(url, { headers: { 'X-API-Key': 'sd_live_not-a-real-key' }, data: {} })).status()).toBe(
      401,
    );
  });

  test('revokedKeyStopsWorking', async ({ request }) => {
    const merchant = await approvedMerchant(request);
    const keys = await request.get(`${API}/merchants/me/api-keys`, { headers: bearer(merchant.session) });
    const [key] = (await keys.json()) as { id: string }[];
    await expectStatus(
      await request.delete(`${API}/merchants/me/api-keys/${key.id}`, { headers: bearer(merchant.session) }),
      204,
    );
    const response = await request.post(`${API}/merchant-api/orders`, {
      headers: { 'X-API-Key': merchant.apiKey },
      data: {},
    });
    expect(response.status()).toBe(401);
  });

  test('merchantApiIsRateLimitedPerKey', async ({ request }) => {
    const merchant = await approvedMerchant(request);
    // Default burst is 20 with 10/s refill; 60 at once must hit the limit. Empty bodies, so nothing is created.
    const statuses = await Promise.all(
      Array.from({ length: 60 }, () =>
        request
          .post(`${API}/merchant-api/orders`, { headers: { 'X-API-Key': merchant.apiKey }, data: {} })
          .then((r) => r.status()),
      ),
    );
    expect(statuses).toContain(429);
    expect(statuses).toContain(400);
  });
});
