# System testing

How SwiftDrop is tested as a whole system, on top of the per-service tests in AGENTS.md §7.
How to run the tests: [`e2e/README.md`](../e2e/README.md).

## 1. Why

Each service has unit, MockMvc and Testcontainers tests that run on every PR. They cannot see problems that only
appear when everything runs together: gateway routing and JWT checks, the outbox → RabbitMQ → consumer chain,
webhooks, server-sent events through the gateway and nginx, MinIO uploads and the real frontend.
The `e2e/` suites cover that layer. They change no service code.

## 2. Test layers

| Layer | Project | Covers | When |
|---|---|---|---|
| API | `api` | Endpoint × role authorization matrix (§5 API surface × 5 roles + anonymous); `/internal/**` and JWKS not routed; tampered and `alg: none` JWTs; refresh-token rotation and logout; API-key revocation and per-key rate limit; RFC 7807 errors without personal data or internals; customer and merchant ownership; fee always from pricing-service | Nightly, and locally before a PR that touches security or the gateway |
| Flows | `flows` | §9.1 customer booking → public tracking → PDF receipt → email; §9.3 assignment → courier lifecycle with proof photo → SSE → emails → daily report; FAILED with reason; §9.2 merchant order → signed webhook → recipient email; idempotent merchant orders | Nightly |
| Resilience | `resilience` | No event lost while notification-service is down (durable queue + outbox); duplicate `eventId` sends one email (rule 7); failed webhook retried and recorded; tracking works after a parcel-service restart | Nightly |
| UI | `ui` | Login per portal, wrong password, portal redirect, public tracking page updating live through nginx | Nightly |

Not covered yet: visual regression, accessibility (axe), load testing, real Google login (the stack runs with
`AUTH_GOOGLE_ENABLED=false`), the AI assistant (needs an LLM key).

## 3. Test data

- Every test creates its own users and data through the public APIs; none writes to a service database (rule 8).
- Staff accounts come from the demo-account seeder (`AUTH_DEMO_ACCOUNTS_PASSWORD`), set by the e2e compose override.
- Couriers and merchants are made per test: sign up, the admin grants the role or approves the merchant, sign in again.
- Fake data only. All mail goes to Mailpit; SMS is a log-only stub.

## 4. Nightly pipeline

`.github/workflows/nightly.yml`, 01:00 Asia/Colombo on `dev`, and on demand (`gh workflow run nightly.yml`):

1. Copy `deploy/compose/.env.example` to `.env` with a throwaway JWT signing key and `INTERNAL_API_TOKEN` generated
   for the run (no repository secrets needed), build and start the stack with the e2e override, wait for every
   health check.
2. Run `api`, `flows` and `ui`, then `resilience` on its own.
3. Always upload the Playwright reports (with traces) and every container's log as artifacts, kept 14 days.
4. A failed scheduled run opens an issue labelled `nightly-failure`, or comments on the open one; the next green run
   closes it.

One retry in CI. A test that only passes on retry is shown as flaky in the report: look at it, don't ignore it.

## 5. Findings from the first runs (14 Nov 2026)

For the owners to decide on; none blocks the tests.

| Area | Finding | Owner |
|---|---|---|
| All services | A forbidden role sending an invalid body gets 400 with the validation errors, not 403: Spring validates `@Valid` bodies before `@PreAuthorize` runs. Authorization still holds (a valid body gets 403), but a caller can learn the request schema of endpoints they cannot use. | Role 1 |
| notification-service | Failed webhook attempts are recorded with `httpStatus: 0` even when the merchant's server answered (e.g. 500 or 404). | Role 6 |
| notification-service | The merchant server's full response body is stored as `failureReason` and shown in `GET /admin/webhook-deliveries`; consider truncating it. | Role 6 |
| auth / merchant | An approved merchant keeps the CUSTOMER role from signup, so it can also book parcels as a customer. Fine if intended; worth stating in §8. | Roles 2, 5 |
