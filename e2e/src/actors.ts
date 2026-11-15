import type { APIRequestContext } from '@playwright/test';
import { API, demoAccounts, env } from './env';
import { bearer, expectStatus, login, setRoles, signup, TEST_PASSWORD, uniq, type Session } from './api';

/**
 * Builds the people a test needs, each one fresh so parallel tests never share state.
 * Everything goes through the public APIs; no test touches a service database (AGENTS.md rule 8).
 */

export const admin = (request: APIRequestContext) => login(request, demoAccounts.admin);
export const dispatcher = (request: APIRequestContext) => login(request, demoAccounts.dispatcher);

export const customer = (request: APIRequestContext) => signup(request, 'customer');

export interface Courier {
  session: Session;
  /** dispatch-service's courier ID, used when assigning (not the user ID). */
  courierId: string;
}

/** A new user given the COURIER role, registered in dispatch-service by opening their day list. */
export async function courier(request: APIRequestContext, adminSession?: Session): Promise<Courier> {
  const user = await signup(request, 'courier');
  await setRoles(request, adminSession ?? (await admin(request)), user.user.id, ['COURIER']);
  const session = await login(request, user.user.email, TEST_PASSWORD);

  const today = await request.get(`${API}/courier/deliveries/today`, { headers: bearer(session) });
  await expectStatus(today, 200, 'courier day list');

  const workload = await request.get(`${API}/dispatch/workload`, { headers: bearer(await dispatcher(request)) });
  await expectStatus(workload, 200, 'dispatch workload');
  const loads = (await workload.json()) as { courierId: string; userId: string }[];
  const own = loads.find((load) => load.userId === session.user.id);
  if (!own) throw new Error('New courier is missing from the dispatch workload');
  return { session, courierId: own.courierId };
}

export interface Merchant {
  session: Session;
  merchantId: string;
  apiKey: string;
  webhookPath: string;
  webhookSecret: string;
}

/** Registers a merchant, has the admin approve it, then creates an API key and a webhook pointing at WireMock. */
export async function approvedMerchant(request: APIRequestContext, adminSession?: Session): Promise<Merchant> {
  const user = await signup(request, 'merchant');
  const id = uniq();
  const registered = await request.post(`${API}/merchants/register`, {
    headers: bearer(user),
    data: {
      businessName: `E2E Shop ${id}`,
      businessRegNo: `E2E-${id}`,
      contactName: 'E2E Contact',
      email: user.user.email,
      phone: '+94770000001',
      address: '3 Market Street, Colombo 11',
    },
  });
  await expectStatus(registered, 201, 'merchant register');
  const merchantId = ((await registered.json()) as { id: string }).id;

  const approve = await request.patch(`${API}/admin/merchants/${merchantId}/approve`, {
    headers: bearer(adminSession ?? (await admin(request))),
  });
  await expectStatus(approve, 200, 'merchant approve');

  // Approval grants the MERCHANT role in auth-service; a new login puts it in the JWT
  const session = await login(request, user.user.email, TEST_PASSWORD);

  const key = await request.post(`${API}/merchants/me/api-keys`, {
    headers: bearer(session),
    data: { name: `e2e-${id}` },
  });
  await expectStatus(key, 201, 'create API key');
  const apiKey = ((await key.json()) as { rawApiKey: string }).rawApiKey;

  const webhookPath = `/hooks/${id}`;
  const hook = await request.put(`${API}/merchants/me/webhook`, {
    headers: bearer(session),
    data: { webhookUrl: `${env.webhookReceiverInternalUrl}${webhookPath}`, rotateSecret: false },
  });
  await expectStatus(hook, 200, 'set webhook');
  const webhookSecret = ((await hook.json()) as { secret?: string }).secret ?? '';

  return { session, merchantId, apiKey, webhookPath, webhookSecret };
}

export const merchantOrder = (overrides: Record<string, unknown> = {}) => ({
  recipientName: 'E2E Shopper',
  recipientPhone: '+94770000002',
  recipientEmail: `e2e.shopper.${uniq()}@swiftdrop.test`,
  pickupAddress: '3 Market Street, Colombo 11',
  dropoffAddress: '4 Lake Road, Galle',
  pickupZone: 'COLOMBO_NORTH',
  dropoffZone: 'GALLE',
  weightKg: 2,
  size: 'MEDIUM',
  speed: 'STANDARD',
  ...overrides,
});
