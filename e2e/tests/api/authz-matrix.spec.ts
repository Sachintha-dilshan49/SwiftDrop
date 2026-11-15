import { test, expect, type APIRequestContext } from '@playwright/test';
import { API, demoAccounts } from '../../src/env';
import { bearer, login, parcelInput, setRoles, signup, TEST_PASSWORD, type Role, type Session } from '../../src/api';
import { approvedMerchant } from '../../src/actors';

/**
 * Every public endpoint (docs/architecture.md §5) called through the gateway by every role.
 *
 * - Anonymous callers must get 401.
 * - Roles that are not allowed must get 403. They send a valid body, because Spring validates the body before the
 *   method security check runs, so an invalid body would answer 400 before the role is ever checked.
 * - Allowed roles must get past authorization: any answer but 401/403/5xx. They send an empty body and a made-up
 *   ID, so they get 400 or 404 and the matrix never creates data.
 */

type Caller = Role | 'ANONYMOUS';
const ROLES: Role[] = ['CUSTOMER', 'MERCHANT', 'COURIER', 'DISPATCHER', 'ADMIN'];
const ALL: Role[] = ROLES;
const PUBLIC = 'PUBLIC' as const;

interface Endpoint {
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  path: string;
  allowed: Role[] | typeof PUBLIC;
  /** A body that passes validation, sent by roles that must be refused. */
  validBody?: Record<string, unknown>;
  multipart?: boolean;
}

// Fixed, so test titles are the same in every worker; no record has this ID
const id = '00000000-0000-4000-8000-000000000e2e';
const today = new Date().toISOString().slice(0, 10);

const endpoints: Endpoint[] = [
  // auth-service
  { method: 'GET', path: '/admin/users', allowed: ['ADMIN'] },
  {
    method: 'PATCH',
    path: `/admin/users/${id}`,
    allowed: ['ADMIN'],
    validBody: { roles: ['ADMIN'], accountStatus: 'ACTIVE' },
  },
  // pricing-service
  { method: 'GET', path: '/pricing/zones', allowed: PUBLIC },
  {
    method: 'GET',
    path: '/pricing/estimate?pickupZone=COLOMBO_SOUTH&dropoffZone=KANDY&weightKg=1&size=SMALL&speed=STANDARD',
    allowed: PUBLIC,
  },
  { method: 'GET', path: '/admin/zones', allowed: ['ADMIN'] },
  {
    method: 'POST',
    path: '/admin/zones',
    allowed: ['ADMIN'],
    validBody: { code: 'ZZ_FORBIDDEN', name: 'Never created', group: 'INTER_CITY', districts: ['Nowhere'] },
  },
  { method: 'DELETE', path: `/admin/zones/${id}`, allowed: ['ADMIN'] },
  { method: 'GET', path: '/admin/pricing-rules', allowed: ['ADMIN'] },
  { method: 'PUT', path: `/admin/pricing-rules/${id}`, allowed: ['ADMIN'], validBody: { amount: 0, active: false } },
  { method: 'GET', path: '/admin/promo-codes', allowed: ['ADMIN'] },
  {
    method: 'POST',
    path: '/admin/promo-codes',
    allowed: ['ADMIN'],
    validBody: { code: 'E2E_FORBIDDEN', discountAmount: 9999, validFrom: new Date().toISOString() },
  },
  // parcel-service
  { method: 'POST', path: '/parcels', allowed: ['CUSTOMER', 'ADMIN'], validBody: { ...parcelInput() } },
  { method: 'GET', path: '/parcels', allowed: ALL },
  { method: 'GET', path: `/parcels/${id}`, allowed: ALL },
  { method: 'GET', path: `/parcels/${id}/receipt`, allowed: ALL },
  { method: 'GET', path: '/track/SD-ZZZZ-ZZZZ', allowed: PUBLIC },
  // dispatch-service
  { method: 'GET', path: '/dispatch/unassigned', allowed: ['DISPATCHER', 'ADMIN'] },
  {
    method: 'POST',
    path: '/dispatch/assignments',
    allowed: ['DISPATCHER', 'ADMIN'],
    validBody: { parcelId: id, courierId: id },
  },
  { method: 'GET', path: '/dispatch/workload', allowed: ['DISPATCHER', 'ADMIN'] },
  { method: 'GET', path: `/reports/daily?from=${today}&to=${today}`, allowed: ['DISPATCHER', 'ADMIN'] },
  { method: 'GET', path: '/courier/deliveries/today', allowed: ['COURIER'] },
  { method: 'GET', path: `/courier/deliveries/${id}`, allowed: ['COURIER'] },
  { method: 'POST', path: `/courier/deliveries/${id}/status`, allowed: ['COURIER'], multipart: true },
  // merchant-service
  { method: 'POST', path: '/merchants/register', allowed: ALL },
  { method: 'GET', path: '/merchants/me', allowed: ALL },
  { method: 'GET', path: '/admin/merchants?status=PENDING', allowed: ['ADMIN'] },
  { method: 'PATCH', path: `/admin/merchants/${id}/approve`, allowed: ['ADMIN'] },
  { method: 'PATCH', path: `/admin/merchants/${id}/reject`, allowed: ['ADMIN'], validBody: { reason: 'e2e' } },
  { method: 'GET', path: '/merchants/me/api-keys', allowed: ['MERCHANT', 'ADMIN'] },
  { method: 'POST', path: '/merchants/me/api-keys', allowed: ['MERCHANT', 'ADMIN'], validBody: { name: 'e2e' } },
  { method: 'DELETE', path: `/merchants/me/api-keys/${id}`, allowed: ['MERCHANT', 'ADMIN'] },
  { method: 'GET', path: '/merchants/me/webhook', allowed: ['MERCHANT', 'ADMIN'] },
  {
    method: 'PUT',
    path: '/merchants/me/webhook',
    allowed: ['MERCHANT', 'ADMIN'],
    validBody: { webhookUrl: 'http://forbidden.invalid/hook', rotateSecret: false },
  },
  { method: 'GET', path: '/merchants/me/orders', allowed: ['MERCHANT', 'ADMIN'] },
  // notification-service
  { method: 'GET', path: '/admin/webhook-deliveries', allowed: ['ADMIN', 'DISPATCHER'] },
  { method: 'POST', path: `/admin/webhook-deliveries/${id}/replay`, allowed: ['ADMIN', 'DISPATCHER'] },
];

let sessions: Record<Role, Session>;

test.beforeAll(async ({ request }) => {
  const admin = await login(request, demoAccounts.admin);
  // A merchant account keeps the CUSTOMER role it signed up with; strip it so this column tests MERCHANT alone
  const merchant = (await approvedMerchant(request, admin)).session;
  await setRoles(request, admin, merchant.user.id, ['MERCHANT']);
  sessions = {
    ADMIN: admin,
    DISPATCHER: await login(request, demoAccounts.dispatcher),
    COURIER: await login(request, demoAccounts.courier),
    CUSTOMER: await signup(request),
    MERCHANT: await login(request, merchant.user.email, TEST_PASSWORD),
  };
});

async function call(request: APIRequestContext, endpoint: Endpoint, caller: Caller, body: Record<string, unknown>) {
  const headers = caller === 'ANONYMOUS' ? {} : bearer(sessions[caller]);
  const payload =
    endpoint.method === 'GET' || endpoint.method === 'DELETE'
      ? {}
      : endpoint.multipart
        ? { multipart: { status: 'PICKED_UP' } }
        : { data: body };
  return request.fetch(`${API}${endpoint.path}`, { method: endpoint.method, headers, ...payload });
}

for (const endpoint of endpoints) {
  const title = `${endpoint.method} ${endpoint.path.split('?')[0].replace(id, '{id}')} enforces its roles`;
  test(title, async ({ request }) => {
    const mismatches: string[] = [];
    for (const caller of ['ANONYMOUS', ...ROLES] as Caller[]) {
      const allowed = endpoint.allowed === PUBLIC || (caller !== 'ANONYMOUS' && endpoint.allowed.includes(caller));
      const status = (await call(request, endpoint, caller, allowed ? {} : (endpoint.validBody ?? {}))).status();

      let expected: string;
      let ok: boolean;
      if (allowed) {
        expected = 'not 401/403/5xx';
        ok = status !== 401 && status !== 403 && status < 500;
      } else if (caller === 'ANONYMOUS') {
        expected = '401';
        ok = status === 401;
      } else {
        expected = '403';
        ok = status === 403;
      }
      if (!ok) mismatches.push(`${caller}: expected ${expected}, got ${status}`);
    }
    expect(mismatches, title).toEqual([]);
  });
}
