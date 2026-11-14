import { env, demoAccounts } from './env';

/**
 * Fails fast with a clear message when the stack is not up or not started with the e2e override,
 * instead of letting every test fail on its own.
 */
export default async function globalSetup() {
  const health = await fetch(`${env.gatewayUrl}/actuator/health`).catch(() => null);
  if (!health?.ok) {
    throw new Error(
      `Gateway not reachable at ${env.gatewayUrl}. Start the stack first: npm run stack:up && npm run stack:wait`,
    );
  }
  const login = await fetch(`${env.gatewayUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ emailOrMobile: demoAccounts.admin, password: env.demoPassword }),
  });
  if (login.status !== 200) {
    throw new Error(
      `Demo admin login failed (HTTP ${login.status}). Start the stack with the e2e override ` +
        '(npm run stack:up), which sets AUTH_DEMO_ACCOUNTS_PASSWORD to E2E_DEMO_PASSWORD.',
    );
  }

  // Every test merchant gets a webhook URL under /hooks/. Answer 200 to all of them by default, so merchants made
  // for other tests do not pile up failed deliveries. Tests that check webhooks add their own, higher-priority stub.
  const stub = await fetch(`${env.webhookReceiverAdminUrl}/__admin/mappings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority: 10, request: { method: 'POST', urlPattern: '/hooks/.*' }, response: { status: 200 } }),
  });
  if (!stub.ok) throw new Error(`Webhook receiver not reachable at ${env.webhookReceiverAdminUrl}`);
}
