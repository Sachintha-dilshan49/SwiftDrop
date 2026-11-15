# e2e: system tests

Playwright tests that run against the **whole stack** (gateway, every service, RabbitMQ, Mailpit, MinIO, frontend)
started with Docker Compose. Per-service unit and integration tests stay in each module; these check what only
shows up when everything runs together. Background and scope: [`docs/testing.md`](../docs/testing.md).

## Run it locally

Needs Docker (about 8 GB of memory for the stack) and Node 20+.

```bash
cd e2e
npm ci
npx playwright install chromium       # once, for the UI tests

npm run stack:up                      # build + start the stack with the e2e override
npm run stack:wait                    # until every service is healthy

npm test                              # api + flows (about 20 s)
npm run test:ui                       # browser smoke tests
npm run test:resilience               # stops/starts containers; run it on its own
npm run test:all                      # everything, resilience last

npm run report                        # open the HTML report (traces for failures)
npm run stack:logs                    # write each container's log to e2e/stack-logs/
npm run stack:down                    # stop and delete the e2e volumes
```

`stack:up` uses the same container names and ports as the normal stack, so it replaces a running local stack.
Its databases live in their own volumes (`swiftdrop_e2e_*`), so `stack:down` never deletes your dev data.
Bring your normal stack back with `docker compose -f deploy/compose/docker-compose.yml up -d`.

## What the override changes

[`compose/docker-compose.e2e.yml`](compose/docker-compose.e2e.yml), on top of `deploy/compose/docker-compose.yml`:

- adds `webhook-receiver` (WireMock, port 8089) as the merchants' webhook endpoint;
- turns on the demo accounts (`demo.admin@`, `demo.dispatcher@`, `demo.courier@swiftdrop.example.com`) with the
  test-only password `E2E_DEMO_PASSWORD` (default `e2e-demo-password-123`);
- forces all mail to Mailpit, even if your `.env` points at a real mail server;
- raises the public estimate rate limit (many tests share one IP); the merchant API limit stays real;
- uses separate volumes.

## Layout

| Folder | Project | What |
|---|---|---|
| `tests/api` | `api` | Role × endpoint authorization matrix, gateway boundaries, JWT tampering, refresh rotation, API keys, rate limit, RFC 7807 errors with no personal data, ownership, server-side pricing |
| `tests/flows` | `flows` | Customer booking → tracking → receipt → email; courier lifecycle with proof photo, SSE and reports; merchant order → signed webhook; idempotency |
| `tests/resilience` | `resilience` | notification-service outage, duplicate events, webhook retry, parcel-service restart |
| `tests/ui` | `ui` | Login, portal access, live tracking page through nginx |
| `src/` | | Helpers: `api.ts` (requests), `actors.ts` (fresh users per test), `outside.ts` (Mailpit, WireMock, SSE, RabbitMQ, Docker) |

## Writing tests

- Every test creates its own users through the public APIs (`src/actors.ts`); never share state between tests and
  never write to a service database.
- Wait for async results with `expect.poll` (or the `waitFor…` helpers), never a fixed sleep.
- Test names describe behaviour: `courierCannotUpdateAnotherCouriersDelivery`.
- Fake data only: `@swiftdrop.test` emails, `+9477000000x` phone numbers.

## Settings

All optional; defaults match the override.

| Variable | Default |
|---|---|
| `E2E_GATEWAY_URL` | `http://localhost:8080` |
| `E2E_FRONTEND_URL` | `http://localhost:3000` |
| `E2E_MAILPIT_URL` | `http://localhost:8025` |
| `E2E_WEBHOOK_ADMIN_URL` | `http://localhost:8089` |
| `E2E_RABBITMQ_MGMT_URL` | `http://localhost:15672` |
| `E2E_DEMO_PASSWORD` | `e2e-demo-password-123` |
| `E2E_JAVA_TOOL_OPTIONS` | empty (CI sets `-Xmx512m`) |
