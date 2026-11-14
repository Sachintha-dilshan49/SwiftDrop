import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { API, env } from './env';

export type Role = 'CUSTOMER' | 'MERCHANT' | 'COURIER' | 'DISPATCHER' | 'ADMIN';

export interface Session {
  accessToken: string;
  refreshToken: string;
  user: { id: string; fullName: string; email: string; roles: Role[] };
}

/** A short unique suffix for emails, names and keys. */
export const uniq = () => randomUUID().slice(0, 8);

export const bearer = (session: Session | string) => ({
  Authorization: `Bearer ${typeof session === 'string' ? session : session.accessToken}`,
});

/** Fails with the response body in the message, which is what you need when a status is wrong. */
export async function expectStatus(response: APIResponse, status: number, what = '') {
  if (response.status() !== status) {
    const body = await response.text().catch(() => '');
    throw new Error(
      `${what || response.url()}: expected HTTP ${status}, got ${response.status()}\n${body.slice(0, 2000)}`,
    );
  }
}

export async function login(request: APIRequestContext, email: string, password = env.demoPassword) {
  const response = await request.post(`${API}/auth/login`, { data: { emailOrMobile: email, password } });
  await expectStatus(response, 200, `login ${email}`);
  return (await response.json()) as Session;
}

export const TEST_PASSWORD = 'e2e-Passw0rd-long-enough';

/** Signs up a new CUSTOMER with a unique email. */
export async function signup(request: APIRequestContext, label = 'customer') {
  const id = uniq();
  const response = await request.post(`${API}/auth/signup`, {
    data: { fullName: `E2E ${label} ${id}`, email: `e2e.${label}.${id}@swiftdrop.test`, password: TEST_PASSWORD },
  });
  await expectStatus(response, 201, 'signup');
  return (await response.json()) as Session;
}

/** Admin sets a user's roles; the user must log in again for the new roles to be in their JWT. */
export async function setRoles(request: APIRequestContext, admin: Session, userId: string, roles: Role[]) {
  const response = await request.patch(`${API}/admin/users/${userId}`, {
    headers: bearer(admin),
    data: { roles, accountStatus: 'ACTIVE' },
  });
  await expectStatus(response, 200, 'admin set roles');
}

export interface ParcelInput {
  pickupAddress: string;
  pickupZone: string;
  dropoffAddress: string;
  dropoffZone: string;
  recipientName: string;
  recipientPhone: string;
  size: 'SMALL' | 'MEDIUM' | 'LARGE' | 'EXTRA_LARGE';
  weightKg: number;
  speed: 'STANDARD' | 'EXPRESS' | 'NEXT_DAY';
  promoCode?: string;
}

/** Test data only: fake addresses and a number from the reserved test range. */
export const parcelInput = (overrides: Partial<ParcelInput> = {}): ParcelInput => ({
  pickupAddress: '1 Test Lane, Colombo 03',
  pickupZone: 'COLOMBO_SOUTH',
  dropoffAddress: '2 Sample Road, Kandy',
  dropoffZone: 'KANDY',
  recipientName: 'E2E Recipient',
  recipientPhone: '+94770000000',
  size: 'SMALL',
  weightKg: 1.5,
  speed: 'STANDARD',
  ...overrides,
});

export interface Parcel {
  id: string;
  trackingNo: string;
  customerId: string;
  courierId: string | null;
  status: string;
  feeAmount: number;
  feeCurrency: string;
  history: { status: string; note: string | null; proofReference: string | null }[];
}

export async function createParcel(request: APIRequestContext, customer: Session, input = parcelInput()) {
  const response = await request.post(`${API}/parcels`, { headers: bearer(customer), data: input });
  await expectStatus(response, 201, 'create parcel');
  return (await response.json()) as Parcel;
}

export async function getParcel(request: APIRequestContext, session: Session, id: string) {
  const response = await request.get(`${API}/parcels/${id}`, { headers: bearer(session) });
  await expectStatus(response, 200, 'get parcel');
  return (await response.json()) as Parcel;
}

/** A 1x1 PNG, small enough to send as a proof photo. */
export const tinyPng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
  'base64',
);

export async function courierUpdate(
  request: APIRequestContext,
  courier: Session,
  assignmentId: string,
  status: string,
  extra: { note?: string; failureReason?: string; recipientName?: string; photo?: Buffer } = {},
) {
  const multipart: Record<string, string | { name: string; mimeType: string; buffer: Buffer }> = { status };
  if (extra.note) multipart.note = extra.note;
  if (extra.failureReason) multipart.failureReason = extra.failureReason;
  if (extra.recipientName) multipart.recipientName = extra.recipientName;
  if (extra.photo) multipart.photo = { name: 'proof.png', mimeType: 'image/png', buffer: extra.photo };
  return request.post(`${API}/courier/deliveries/${assignmentId}/status`, { headers: bearer(courier), multipart });
}

/** Checks the response is an RFC 7807 ProblemDetail with the given status. */
export async function expectProblem(response: APIResponse, status: number) {
  await expectStatus(response, status);
  expect(response.headers()['content-type'] ?? '').toContain('application/problem+json');
  const problem = await response.json();
  expect(problem).toMatchObject({ status, title: expect.any(String) });
  return problem as { status: number; title: string; detail?: string; type?: string; [key: string]: unknown };
}
