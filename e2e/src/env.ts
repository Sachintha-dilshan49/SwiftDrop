/** Where the stack is and how to log in to it. Defaults match e2e/compose/docker-compose.e2e.yml. */
export const env = {
  gatewayUrl: process.env.E2E_GATEWAY_URL ?? 'http://localhost:8080',
  frontendUrl: process.env.E2E_FRONTEND_URL ?? 'http://localhost:3000',
  mailpitUrl: process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025',
  webhookReceiverAdminUrl: process.env.E2E_WEBHOOK_ADMIN_URL ?? 'http://localhost:8089',
  /** Address the services use to reach the receiver inside the Docker network. */
  webhookReceiverInternalUrl: process.env.E2E_WEBHOOK_INTERNAL_URL ?? 'http://webhook-receiver:8080',
  rabbitMgmtUrl: process.env.E2E_RABBITMQ_MGMT_URL ?? 'http://localhost:15672',
  rabbitUser: process.env.E2E_RABBITMQ_USER ?? 'guest',
  rabbitPassword: process.env.E2E_RABBITMQ_PASSWORD ?? 'guest',
  /** Same value as AUTH_DEMO_ACCOUNTS_PASSWORD in the e2e override. Test-only, never a real password. */
  demoPassword: process.env.E2E_DEMO_PASSWORD ?? 'e2e-demo-password-123',
  composeFiles: ['../deploy/compose/docker-compose.yml', 'compose/docker-compose.e2e.yml'],
};

export const API = '/api/v1';

export const demoAccounts = {
  admin: 'demo.admin@swiftdrop.example.com',
  dispatcher: 'demo.dispatcher@swiftdrop.example.com',
  courier: 'demo.courier@swiftdrop.example.com',
} as const;

/** Unique per test run, so re-runs against the same stack never collide. */
export const runId = process.env.E2E_RUN_ID ?? Date.now().toString(36);
