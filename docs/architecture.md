# SwiftDrop Architecture

Version 1.2 · 31 October 2026 · Owner: Role 1 (team lead)

Changes in 1.2: table names match the Flyway migrations (§4); merchant and auth endpoints match the code (§5); the fee estimate is public and the Google OAuth paths are forwarded without the `/api/v1` prefix (§6.1, §10.4); `auth_db` holds identity data only (§4, ADR-011).

This is the source of truth for how the system is built. Changes to anything here need a PR and, for big changes, an entry in [`decisions.md`](decisions.md).

---

## 1. Goals and constraints

From the assignment brief:

- Four user types: Customer, Merchant, Courier, Dispatcher/Admin, with role-based access.
- JWT authentication, plus OAuth 2.0 for "Login with Google" and merchant API access.
- Real-time parcel tracking (we use Server-Sent Events).
- At least one asynchronous integration pattern: status events through a message broker trigger notifications and merchant webhooks.
- Merchant webhooks retried on failure.
- Everything containerized and runnable with Docker Compose; Kubernetes is a bonus.
- Secure handling of personal data: validation, hashed passwords, nothing sensitive in errors or logs.

### Wording

The person who picks up and delivers parcels is always a **Courier**: in the UI, API paths, code, database names, events and docs. Never "rider" or "driver". The role is `COURIER`, the service is `dispatch-service` and the courier screens live under `/courier`.

## 2. Architecture style

**Microservices behind an API gateway**, with these simplifications to keep it manageable for a student team:

| Simplification | Why |
|---|---|
| No service registry (Eureka) | Docker Compose and Kubernetes DNS resolve service names |
| No config server | Configuration from environment variables, which the brief requires anyway |
| One PostgreSQL instance, separate database per service | Keeps the database-per-service rule without seven DB containers |
| Shared `common` library | One implementation of error handling, security config, event DTOs and log masking (DRY) |
| Monorepo, Maven multi-module | One clone, one build, consistent versions |

### Why microservices for this project

- Each of the nine roles owns a service, so ownership, testing and the demo split cleanly.
- The integration patterns the brief marks (pub/sub, broker, webhooks) are real network boundaries, not simulated ones.
- The Kubernetes bonus is meaningful with several independently deployable services.

### Layers

| Layer | Components |
|---|---|
| Presentation | React SPA (all roles), mobile-first public tracking page |
| Edge | Ingress (Kubernetes) and `api-gateway` |
| Business logic | Seven Spring Boot services, each layered Controller → Service → Repository |
| Integration | RabbitMQ exchange and queues, notification consumer, webhook sender |
| Data | PostgreSQL, Redis, MinIO |

## 3. System diagram

```mermaid
flowchart TB
  subgraph Clients
    C[Customer browser]
    T[Public tracking page]
    K[Courier phone]
    D[Dispatcher / Admin]
    M[Merchant store server]
  end

  FE[frontend<br/>React SPA · nginx]
  GW[api-gateway :8080<br/>JWT check · routing · rate limits]

  C & T & K & D --> FE
  FE -- REST + SSE --> GW
  M -- X-API-Key --> GW

  subgraph Services
    AUTH[auth-service :8081]
    PRICE[pricing-service :8082]
    PARCEL[parcel-service :8083]
    DISP[dispatch-service :8084]
    MERCH[merchant-service :8085]
    NOTIF[notification-service :8086]
    AI[ai-assistant :8087]
  end

  GW --> AUTH & PRICE & PARCEL & DISP & MERCH & NOTIF & AI
  PARCEL -- fee --> PRICE
  DISP -- change status --> PARCEL
  MERCH -- create order --> PARCEL

  subgraph RabbitMQ
    EX{{parcel.events<br/>topic exchange}}
    Q1[notification.q]
    Q2[webhook.q]
    Q3[dispatch.q]
    Q4[tracking.q]
  end

  PARCEL -. publish ParcelStatusChanged .-> EX
  EX -.-> Q1 & Q2 & Q3 & Q4
  Q1 -.-> NOTIF
  Q2 -.-> NOTIF
  Q3 -.-> DISP
  Q4 -.-> PARCEL

  AUTH --> G[(Google OAuth 2.0)]
  AI --> L[(LLM provider)]
  NOTIF -- HMAC-signed POST --> W[(Merchant webhook URL)]
  NOTIF --> MAIL[(Mailpit SMTP / mock SMS)]
  DISP --> S3[(MinIO delivery-proofs)]
  GW --> R[(Redis)]
```

Detailed diagrams: [`diagrams/architecture/`](diagrams/architecture/), [`diagrams/integration/`](diagrams/integration/) and the design report [`report/SwiftDrop_Design_Report.md`](report/SwiftDrop_Design_Report.md). The older `diagrams/architecture-overview.html` is out of date.

## 4. Services

| Service | Port | Owner | Database | Responsibility |
|---|---|---|---|---|
| `api-gateway` | 8080 | Role 1 | — (Redis) | Single entry point, JWT verification, routing, CORS, rate limits, request IDs |
| `auth-service` | 8081 | Role 2 | `auth_db` | Signup/login, BCrypt, JWT issuing, refresh tokens, Google OAuth 2.0, roles, user admin |
| `pricing-service` | 8082 | Role 1 | `pricing_db` | Delivery zones, pricing rules, fee estimate |
| `parcel-service` | 8083 | Role 3 | `parcel_db` | Parcels, tracking numbers, status machine, history, receipt PDF, SSE stream, event publishing |
| `dispatch-service` | 8084 | Role 4 | `dispatch_db` | Couriers, assignments, workloads, courier day list, proof uploads, reports |
| `merchant-service` | 8085 | Role 5 | `merchant_db` | Merchant registration and approval, API keys, merchant order API, webhook settings |
| `notification-service` | 8086 | Role 6 | `notification_db` | Event consumers, email, mock SMS, webhook delivery with retry |
| `ai-assistant` | 8087 | Role 9 | — | Chat endpoint, MCP tools that call our APIs |
| `frontend` | 80 | Role 7 + Role 1 | — | React SPA for all roles |

### Main tables per database

| Database | Tables |
|---|---|
| `auth_db` | `users`, `roles`, `user_roles`, `refresh_tokens`, `oauth_accounts`, `oauth_login_codes` |
| `pricing_db` | `zones`, `zone_districts`, `pricing_rules`, `promo_codes` |
| `parcel_db` | `parcels`, `parcel_status_history`, `outbox_events`, `processed_events` |
| `dispatch_db` | `couriers`, `assignments`, `delivery_proofs`, `daily_stats`, `processed_events` |
| `merchant_db` | `merchants`, `merchant_api_keys`, `merchant_webhook_endpoints`, `merchant_orders` |
| `notification_db` | `notifications`, `webhook_deliveries`, `processed_events` |

`auth_db` holds identity only: accounts, roles, tokens and linked Google accounts. Merchant business details live in `merchant_db.merchants` and courier details (vehicle, zone) in `dispatch_db.couriers` (ADR-011).

ER diagrams: [`diagrams/er/`](diagrams/er/) (one per database plus an overview) and [`diagrams/er-parcel-service.puml`](diagrams/er-parcel-service.puml).

## 5. API surface

All public routes are prefixed `/api/v1` and go through the gateway. The exceptions are Spring Security's Google OAuth paths `/oauth2/**` and `/login/oauth2/**`, which the gateway forwards to auth-service unchanged. `/internal/**` routes are service-to-service only and are **not** exposed by the gateway.

| Service | Endpoints |
|---|---|
| auth | `POST /auth/signup` · `POST /auth/login` (email or mobile number) · `POST /auth/refresh` · `POST /auth/logout` · `GET /oauth2/authorization/google` and `GET /login/oauth2/code/google` (no `/api/v1` prefix, only when `AUTH_GOOGLE_ENABLED=true`) · `POST /auth/token` (exchange one-time code) · `GET, PATCH /admin/users` · internal `GET /.well-known/jwks.json` (no `/api/v1` prefix) · internal `GET /internal/users/{id}/contact` |
| pricing | `GET /pricing/estimate?fromZone&toZone&weightKg&size&speed&promoCode` · CRUD `/admin/zones` · CRUD `/admin/pricing-rules` · internal `POST /internal/pricing/quote` (rules in [§8](#8-pricing)) |
| parcel | `POST /parcels` · `GET /parcels` (mine) · `GET /parcels/{id}` · `GET /parcels/{id}/receipt` (PDF) · `GET /track/{trackingNo}` (public) · `GET /track/{trackingNo}/stream` (SSE, public) · internal `POST /internal/parcels` · internal `PATCH /internal/parcels/{id}/status` · internal `GET /internal/parcels/{id}/contacts` |
| dispatch | `GET /dispatch/unassigned` · `POST /dispatch/assignments` · `GET /dispatch/workload` · `GET /reports/daily?from&to` · `GET /courier/deliveries/today` · `POST /courier/deliveries/{id}/status` (multipart, optional photo) |
| merchant | `POST /merchants/register` · `GET /merchants/me` · `GET /admin/merchants?status=PENDING` · `PATCH /admin/merchants/{id}/approve` · `PATCH /admin/merchants/{id}/reject` · `POST, GET /merchants/me/api-keys` · `DELETE /merchants/me/api-keys/{id}` · `GET, PUT /merchants/me/webhook` · `POST /merchant-api/orders` (X-API-Key, full path `/api/v1/merchant-api/orders`) · `GET /merchants/me/orders` · `GET /merchants/me/orders/{id}` · internal `GET /internal/merchants/{id}/webhook` |
| notification | `GET /admin/webhook-deliveries` · `POST /admin/webhook-deliveries/{id}/replay` |
| ai-assistant | `POST /assistant/chat` · MCP endpoint `/mcp` (tools: `track_parcel`, `estimate_fee`, `my_parcels`) |

Each service publishes its OpenAPI spec at `/v3/api-docs`; the gateway aggregates them in Swagger UI.

## 6. Communication

There are three ways traffic moves, and each has one job:

| From → to | How | Pattern |
|---|---|---|
| Browser, merchant server → system | HTTPS through the **API gateway only** | Facade / single entry point |
| Service → service, answer needed now | Direct REST/HTTP + JSON on the private network | Request/reply |
| Service → services, "this happened" | RabbitMQ `parcel.events` | Publish/subscribe |

### 6.1 External clients → API gateway

External clients (the SPA in a browser, merchant store servers) reach the system **only** through `api-gateway`. Service ports are not exposed outside the private network.

The gateway does:

- **Routing** by path prefix (table below). The `/api/v1` prefix is kept; services see the full path.
- **JWT check** on every non-public route, using auth-service's JWKS (fetched once and cached, refreshed on unknown key IDs).
- **CORS** for the SPA origin.
- **`X-Request-Id`**: creates one if the client did not send it and forwards it; every service logs it.
- **Rate limiting** with Redis for `/api/v1/merchant-api/**`, per API key, and for the public `GET /api/v1/pricing/estimate`, per client IP.
- **SSE pass-through** for `/api/v1/track/{trackingNo}/stream` without response buffering.
- **Public routes** (no JWT): `POST /api/v1/auth/signup`, `/login`, `/refresh`, `/logout` and `/token`; `GET /api/v1/pricing/estimate` (guests and the AI assistant, rate-limited; booking still needs a login); `/api/v1/track/**`; and the Google OAuth paths `/oauth2/**` and `/login/oauth2/**`, forwarded unchanged without the `/api/v1` prefix so Spring Security's default redirect URI works.
- **API-key route**: `/api/v1/merchant-api/**` carries `X-API-Key` instead of a JWT. The gateway rejects requests without an `X-API-Key` header (`401`) and rate-limits per key; merchant-service verifies the key itself. (Built in the gateway task.)

The gateway does **not** contain business logic or ownership checks (those are in the services, see §9), and it **never routes `/internal/**`**.

| Path | Service |
|---|---|
| `/api/v1/auth/**`, `/api/v1/admin/users/**` | `auth-service:8081` |
| `/oauth2/**`, `/login/oauth2/**` (forwarded unchanged, no prefix) | `auth-service:8081` |
| `/api/v1/pricing/**`, `/api/v1/admin/zones/**`, `/api/v1/admin/pricing-rules/**` | `pricing-service:8082` |
| `/api/v1/parcels/**`, `/api/v1/track/**` | `parcel-service:8083` |
| `/api/v1/dispatch/**`, `/api/v1/courier/**`, `/api/v1/reports/**` | `dispatch-service:8084` |
| `/api/v1/merchants/**`, `/api/v1/merchant-api/**`, `/api/v1/admin/merchants/**` | `merchant-service:8085` |
| `/api/v1/admin/webhook-deliveries/**` | `notification-service:8086` |
| `/api/v1/assistant/**` | `ai-assistant:8087` |
| `/internal/**` | not routed |

### 6.2 Service-to-service (request/reply over REST)

Used when the caller needs the answer before it can respond. Services call each other **directly** with Spring `RestClient`, HTTP + JSON, on the private network by service name (e.g. `http://pricing-service:8082`). Not through the gateway, not gRPC.

- Base URLs come from environment variables: `AUTH_SERVICE_URL`, `PRICING_SERVICE_URL`, `PARCEL_SERVICE_URL`, `MERCHANT_SERVICE_URL`.
- Timeouts: 2 s connect, 5 s read. A timeout or 5xx from the callee becomes `503 Service Unavailable` for the caller's client.
- Calls to `/internal/**` carry `X-Internal-Token` (see below). The shared `InternalRestClientFactory` in `common` sets it up (`docs/conventions.md`, "Calling another service").

| Caller | Callee | Why |
|---|---|---|
| parcel | pricing | `POST /internal/pricing/quote`: the fee and its breakdown when a parcel is created, for customer bookings and merchant orders (never trust the client's price) |
| merchant | parcel | Create a parcel for a merchant order |
| dispatch | parcel | Change parcel status (parcel owns the rules) |
| notification | auth, merchant, parcel | Look up contact details and webhook URL/secret (events carry IDs only) |
| ai-assistant | gateway | Call public APIs with the user's token |

#### Internal endpoints

| Method | Path | Service | Used by |
|---|---|---|---|
| `GET` | `/internal/users/{id}/contact` | auth-service | notification |
| `GET` | `/internal/merchants/{id}/webhook` | merchant-service | notification |
| `GET` | `/internal/parcels/{id}/contacts` | parcel-service | notification |
| `POST` | `/internal/parcels` | parcel-service | merchant |
| `PATCH` | `/internal/parcels/{id}/status` | parcel-service | dispatch |
| `POST` | `/internal/pricing/quote` | pricing-service | parcel |

All of them require the header `X-Internal-Token` equal to the environment variable `INTERNAL_API_TOKEN`; missing or wrong → `401` ProblemDetail. A user's JWT does not open them. `common` enforces this for every servlet service (ADR-010). Internal endpoints are left out of the public OpenAPI docs.

### 6.3 Asynchronous (publish/subscribe over RabbitMQ)

- Exchange `parcel.events`, type **topic**, durable.
- Routing key: `parcel.status.<status>` in lower case, e.g. `parcel.status.out_for_delivery`.
- Only `parcel-service` publishes, using the **transactional outbox**: the event row is written in the same transaction as the status change and a scheduled relay publishes it, then marks it sent.

| Queue | Binding | Consumer | Does |
|---|---|---|---|
| `notification.q` | `parcel.status.#` | notification-service | Email on every change; SMS on OUT_FOR_DELIVERY, DELIVERED, FAILED |
| `webhook.q` | `parcel.status.#` | notification-service | POST to merchant webhook if `merchantId` is set |
| `dispatch.q` | `parcel.status.#` | dispatch-service | Update daily counters for reports |
| `tracking.q` | `parcel.status.#` | parcel-service | Push the change to open SSE connections |

### Who is notified

| Parcel booked by | Email | SMS | Webhook |
|---|---|---|---|
| Customer (`merchantId` is null) | the customer | the recipient | — |
| Merchant (`merchantId` set) | the recipient | the recipient | signed POST to the merchant's webhook URL |

- Email on every status change; SMS on `OUT_FOR_DELIVERY`, `DELIVERED` and `FAILED`.
- Events carry IDs only, so notification-service looks up contacts at send time: the customer from auth-service (`/internal/users/{id}/contact`), the recipient from parcel-service (`/internal/parcels/{id}/contacts`), the webhook URL and secret from merchant-service (`/internal/merchants/{id}/webhook`).

### Webhook retry

1. Delivery fails (timeout, non-2xx) → message goes to `webhook.retry.10s` (TTL 10 s, dead-letters back to `webhook.q`).
2. Next failure → `webhook.retry.1m`, then `webhook.retry.5m`.
3. After the third retry → `webhook.parking`. An admin can replay it from the dashboard.
4. Every attempt is recorded in `webhook_deliveries` (status code, attempt number, time).
5. Requests carry `X-SwiftDrop-Signature: sha256=<HMAC of body with merchant secret>`, `X-SwiftDrop-Event-Id` and `X-SwiftDrop-Timestamp`.

### Event contract (`common` module, `ParcelStatusChanged` v1)

```json
{
  "eventId": "8f2c1e0a-3b5d-4c2e-9a71-0d6f2b9c1e44",
  "type": "ParcelStatusChanged",
  "version": 1,
  "occurredAt": "2026-10-21T09:15:00Z",
  "parcelId": "c41d…",
  "trackingNo": "SD-7K3Q-9P2M",
  "customerId": "a9e0…",
  "merchantId": "5b17…",
  "courierId": "e3f8…",
  "fromStatus": "IN_TRANSIT",
  "toStatus": "OUT_FOR_DELIVERY",
  "note": "Left Kandy hub",
  "failureReason": null
}
```

- IDs only. No names, phone numbers or addresses travel in events.
- `merchantId` is null for customer bookings; `courierId` is null before assignment.
- Consumers are idempotent: they store `eventId` in `processed_events` and skip duplicates.

### Real-time tracking

- Server-Sent Events: `GET /api/v1/track/{trackingNo}/stream`.
- The browser uses `EventSource`; it reconnects automatically.
- parcel-service keeps an emitter per open page; the `tracking.q` consumer pushes each change to matching emitters.
- Heartbeat comment every 20 s so proxies keep the connection open.

## 7. Parcel status machine

```
CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
                                                   ↘ FAILED (reason required)
```

- Any other transition returns `409 Conflict`.
- Only the assigned courier (or a dispatcher/admin) can move a parcel past CREATED.
- Every change writes a row to `parcel_status_history` with who, when, note and optional proof reference.

## 8. Pricing

```
fee = route base fee + extra weight + size surcharge + express surcharge − promo
```

The fee is calculated **only in pricing-service**. A price sent by the browser or a merchant is never trusted. All amounts are LKR.

| Endpoint | Who calls it | Purpose |
|---|---|---|
| `GET /api/v1/pricing/estimate` (public, through the gateway, rate-limited per client) | Guests, booking screen, AI assistant (`estimate_fee`) | Show the expected fee before booking |
| `POST /internal/pricing/quote` (internal, `X-Internal-Token`) | parcel-service, for customer bookings and merchant orders | The binding fee when the parcel is created |

Both use the same rules below. parcel-service stores the quote's `total` **and** its breakdown with the parcel, so the receipt shows how the fee was made up even if the rules change later.

```json
// POST /internal/pricing/quote
{ "pickupZone": "COLOMBO_SOUTH", "dropoffZone": "COLOMBO_CENTRAL", "weightKg": 1.2,
  "size": "SMALL", "speed": "EXPRESS", "promoCode": "DROP100" }

// 200 OK
{ "routeType": "NEIGHBOURING", "baseFee": 350.00, "weightFee": 40.00, "sizeFee": 0.00,
  "expressFee": 100.00, "discount": 100.00, "total": 390.00, "currency": "LKR" }
```

`promoCode` is optional. An unknown or expired promo code, or a weight that does not fit the size, returns `400` ProblemDetail.

### Zones

| Zone | Areas | Group |
|---|---|---|
| Colombo Central | Colombo 01–03 | `COLOMBO` |
| Colombo South | Colombo 04–06 | `COLOMBO` |
| Colombo North | Colombo 07–15 | `COLOMBO` |
| Kotte & Rajagiriya | | `SUBURBS` |
| Nugegoda & Maharagama | | `SUBURBS` |
| Dehiwala & Mt Lavinia | | `SUBURBS` |
| Inter-city | Kandy, Galle, Negombo | `INTER_CITY` |

### Route base fee

The route type comes from the pickup and drop-off zones, checked in this order:

| Route type | When | Base fee |
|---|---|---|
| `INTER_CITY` | either end is in the `INTER_CITY` group | 650 |
| `SAME_ZONE` | both ends in the same zone | 300 |
| `NEIGHBOURING` | different zones in the same group (e.g. Colombo South → Colombo Central) | 350 |
| `COLOMBO_SUBURBS` | one end `COLOMBO`, the other `SUBURBS` | 400 |

### Weight

- The first kg is included; each extra kg costs 40, rounded **up** to the next whole kg (1.2 kg → 1 extra kg → 40).
- Maximum 40 kg.

### Size

| Size | Surcharge | Max weight |
|---|---|---|
| `SMALL` | +0 | 5 kg |
| `MEDIUM` | +100 | 10 kg |
| `LARGE` | +250 | 20 kg |
| `EXTRA_LARGE` | +500 | 40 kg |

The weight must fit the size; otherwise the request fails with `400` (validation error).

### Speed

| Speed | Promise | Surcharge |
|---|---|---|
| `EXPRESS` | within 3 hours | +100 |
| `STANDARD` | today by 8 PM | +0 |
| `NEXT_DAY` | tomorrow by 6 PM | +0 |

### Promo codes

A promo code in `promo_codes` takes a fixed amount off, e.g. `DROP100` = −100. The fee never goes below 0: the `discount` is at most the fee before the promo.

### Worked example

Colombo South → Colombo Central, 1.2 kg, `SMALL`, `EXPRESS`:

| Part | Amount |
|---|---|
| Base fee (`NEIGHBOURING`) | 350 |
| Extra weight (1 kg) | 40 |
| Size (`SMALL`) | 0 |
| Express | 100 |
| **Fee** | **490** |
| Promo `DROP100` | −100 |
| **Fee with promo** | **390** |

## 9. Security

| Concern | Decision |
|---|---|
| Passwords | BCrypt (strength 12). Google users have no password. |
| Tokens | Access JWT signed with RS256, 15 min. Refresh token 7 days, rotated on use, stored hashed. |
| Token verification | Gateway and every service verify user JWTs with the public key from `auth-service` JWKS; no shared secret for user tokens. The JWKS endpoint `GET /.well-known/jwks.json` is internal: services call auth-service directly (`JWT_JWK_SET_URI`, e.g. `http://auth-service:8081/.well-known/jwks.json` in Docker, `http://localhost:8081/...` locally); the gateway does not route it. |
| Roles | `CUSTOMER`, `MERCHANT`, `COURIER`, `DISPATCHER`, `ADMIN` in the `roles` claim; `@PreAuthorize` on endpoints. |
| Ownership | Checked in the service layer: courier ↔ assignment, customer ↔ parcel, merchant ↔ order. |
| Service-to-service | `/internal/**` requires `X-Internal-Token` = `INTERNAL_API_TOKEN`, compared in constant time; callers get `ROLE_INTERNAL_SERVICE`. A user JWT does not open it. Services refuse to start without the token (ADR-010). |
| Merchant API | `X-API-Key`, shown once, stored as SHA-256 hash, prefix kept for lookup, rate-limited per key in the gateway. Merchant must be APPROVED. |
| Google login | Authorization Code flow handled by auth-service; SPA receives a one-time code and exchanges it for our tokens. |
| Input | Bean Validation on all DTOs; frontend validates with zod too. |
| Errors | RFC 7807 ProblemDetail from the shared handler; no stack traces, SQL or personal data. |
| Logs | Logback masking for phone numbers, emails and addresses; request ID in every line. |
| Secrets | Environment variables (`.env` locally, Kubernetes Secrets in the cluster). Nothing in Git. |
| Transport | HTTPS at the Ingress in Kubernetes; services on an internal network only. |

## 10. Key flows

Sequence diagrams for these are in [`diagrams/sequence/`](diagrams/sequence/) and [`diagrams/seq-create-delivery.puml`](diagrams/seq-create-delivery.puml).

### 10.1 Customer creates a delivery
1. SPA calls `GET /pricing/estimate` and shows the fee.
2. Customer confirms → `POST /parcels` with JWT.
3. Gateway verifies the token and routes to parcel-service.
4. parcel-service gets the binding fee from pricing (`POST /internal/pricing/quote`, never trusts the client), then in one transaction saves the parcel (CREATED) with the fee breakdown, a history row and an outbox row.
5. Relay publishes `parcel.status.created`; API returns `201` with the tracking number.

### 10.2 Merchant creates an order through the API
1. Store server → `POST /merchant-api/orders` with `X-API-Key`.
2. Gateway applies the per-key rate limit; merchant-service verifies the key and that the merchant is APPROVED.
3. merchant-service calls `POST /internal/parcels` with the merchantId; parcel-service gets the fee from `POST /internal/pricing/quote` as in 10.1.
4. CREATED event → `webhook.q` → notification-service POSTs the signed webhook to the merchant.

### 10.3 Courier updates status (triggers notifications)
1. Courier → `POST /courier/deliveries/{id}/status` with note and optional photo.
2. dispatch-service checks the assignment belongs to this courier (403 otherwise) and stores the photo in MinIO.
3. dispatch calls `PATCH /internal/parcels/{id}/status`; parcel-service validates the transition and writes history + outbox.
4. The event fans out: email/SMS, merchant webhook, report counters, and SSE push to open tracking pages.

### 10.4 Login with Google (OAuth 2.0)
1. SPA redirects to `/oauth2/authorization/google`; the gateway forwards it to auth-service unchanged. Google login is offered only when `AUTH_GOOGLE_ENABLED=true`.
2. User signs in with Google; Google redirects back to `/login/oauth2/code/google` (also forwarded unchanged) with an authorization code.
3. auth-service validates Google's response, finds or creates/links the user (role CUSTOMER), and creates a hashed, two-minute, single-use login code.
4. Browser is redirected to the configured SPA callback with the one-time code; SPA calls `POST /api/v1/auth/token` to exchange it for our JWT and refresh token.

## 11. Design patterns

| Pattern | Where |
|---|---|
| Strategy | Pricing components: route base fee, extra weight, size, speed, promo ([§8](#8-pricing)) |
| State | Parcel status transitions |
| Observer / publish-subscribe | Status events to four consumers |
| Repository | Spring Data JPA in every service |
| Adapter | `EmailChannel`, `SmsChannel` behind `NotificationChannel` |
| Facade | API gateway as the single entry point |
| Transactional outbox | Reliable event publishing in parcel-service |
| Template method | Webhook retry handling |
| DRY / SOLID | Shared `common` library; single-purpose services; constructor injection |

## 12. Deployment

### Local (Docker Compose)

| Container | Ports | Notes |
|---|---|---|
| `frontend` | 3000 → 80 | nginx serving the Vite build |
| `api-gateway` | 8080 | Only backend port exposed |
| `*-service` | 8081–8087 | Internal network only (ports exposed in dev for debugging) |
| `postgres` | 5432 | Init script creates the six databases and users |
| `rabbitmq` | 5672, 15672 | Management UI for the demo |
| `redis` | 6379 | Rate limits, cache |
| `minio` | 9000, 9001 | Proof photos, console |
| `mailpit` | 1025, 8025 | SMTP and web inbox |

All containers have health checks; services wait for their dependencies to be healthy.

### Kubernetes (bonus)

- Helm umbrella chart in `deploy/helm/swiftdrop` with a sub-chart per service.
- ConfigMaps for non-secret config, Secrets for passwords, JWT keys, API keys.
- One Deployment + ClusterIP Service per service; StatefulSets (or Bitnami charts) for Postgres, RabbitMQ, Redis, MinIO.
- ingress-nginx: `/` → frontend, `/api` → api-gateway. SSE needs `proxy-buffering: off` on the API route.
- Readiness and liveness probes from Spring Boot Actuator.

### CI (GitHub Actions)

- On every PR to `dev`: build, unit + integration tests (Testcontainers), JaCoCo coverage report, frontend lint + test, Docker build check.
- On merge to `main`: build and push images to GHCR tagged with the version and commit SHA.

## 13. Tech stack

| Area | Choice |
|---|---|
| Frontend | React 18, Vite, TypeScript, Tailwind, React Router, TanStack Query, react-hook-form + zod, Recharts |
| Backend | Java 21, Spring Boot 4.1.x, Spring Security, Spring Cloud Gateway (2025.1.x), Spring Data JPA, Spring AMQP, Flyway, OpenPDF, springdoc-openapi |
| AI | Spring AI 2.x with MCP server |
| Data | PostgreSQL 16, Redis 7, MinIO |
| Testing | JUnit 5, Mockito, Testcontainers, WireMock, Awaitility, JaCoCo, Vitest, React Testing Library |
| DevOps | Docker, Docker Compose, Kubernetes, Helm, ingress-nginx, GitHub Actions, GHCR |
