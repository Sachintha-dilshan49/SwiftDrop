# Conventions

Shared rules so nine people write code that looks like one team wrote it.

## 1. REST API

- Base path `/api/v1`. Breaking changes go to `/api/v2`; old versions stay until clients move.
- Resources are plural nouns in kebab-case: `/parcels`, `/pricing-rules`, `/merchant-api/orders`.
- Actions that are not CRUD use a sub-resource: `POST /courier/deliveries/{id}/status`.
- JSON fields in camelCase. Enums in UPPER_SNAKE_CASE.
- IDs are UUIDs. Tracking numbers look like `SD-7K3Q-9P2M`.
- Dates: ISO-8601 UTC (`2026-10-21T09:15:00Z`).
- Money: `{ "amount": 1250.00, "currency": "LKR" }`.
- Pagination: `?page=0&size=20&sort=createdAt,desc`, response with `content`, `page`, `size`, `totalElements`.

### Status codes

| Code | When |
|---|---|
| 200 | Read or update succeeded |
| 201 | Created (with `Location` header) |
| 204 | Deleted, or action with no body |
| 400 | Validation failed |
| 401 | Missing or invalid token / API key |
| 403 | Authenticated but not allowed (wrong role or not the owner) |
| 404 | Not found (also used when the caller may not know it exists) |
| 409 | Conflict, e.g. invalid status transition, duplicate email |
| 429 | Rate limit exceeded |
| 500 | Unexpected error (generic message only) |

### Error body (RFC 7807, produced by `common`)

```json
{
  "type": "https://swiftdrop.lk/problems/invalid-status-transition",
  "title": "Invalid status transition",
  "status": 409,
  "detail": "Parcel cannot move from CREATED to DELIVERED.",
  "instance": "/api/v1/parcels/c41d…/status",
  "requestId": "b7e1…",
  "errors": [ { "field": "weightKg", "message": "must be greater than 0" } ]
}
```

Never include stack traces, SQL, tokens, emails, phone numbers or addresses.

## 2. Backend code

- Base package `lk.swiftdrop.<service>`, organised by feature:
  ```
  lk.swiftdrop.parcel
  ├── parcel/        controller, service, repository, entity, dto, mapper
  ├── tracking/      SSE emitters, tracking consumer
  ├── outbox/        outbox entity, relay
  └── config/
  ```
- DTOs are records with Bean Validation annotations. Entities never leave the service layer.
- Mapping with MapStruct or small hand-written mappers; pick one per service and stay consistent.
- Constructor injection; classes `final` where possible.
- `@Transactional` on service methods, not controllers.
- Business rules in services or domain objects, never in controllers or repositories.
- Throw exceptions from `common` (`NotFoundException`, `ConflictException`, `ForbiddenException`, `ValidationException`); the shared `@RestControllerAdvice` maps them.
- Logging with SLF4J, parameterised (`log.info("Parcel {} moved to {}", id, status)`), never string concatenation, never personal data.
- No `System.out.println`, no commented-out code in PRs.

## 3. Database and migrations

### Ownership

- **Database per service** (ADR-008): `auth_db`, `pricing_db`, `parcel_db`, `dispatch_db`, `merchant_db`, `notification_db` on one PostgreSQL server. `api-gateway` and `ai-assistant` have no database.
- A service connects **only** to its own database, with its own DB user. Data from another service comes through that service's API or events.
- The **service owner writes their own migrations**. The **team lead reviews every migration** (CODEOWNERS on `db/migration/`).
- Role 8 creates the empty databases and users in the Postgres init script. Flyway creates the tables.

### Flyway

- Files in `services/<service>/src/main/resources/db/migration/`.
- Name: `V<number>__<what>.sql` (two underscores), e.g. `V1__create_parcels.sql`, `V4__add_failure_reason_to_parcels.sql`.
- `spring.jpa.hibernate.ddl-auto: validate`. Hibernate never creates or changes tables.
- **Never edit a migration that is merged to `dev`.** Add a new one. Editing breaks everyone's database with a checksum error.
- Two open branches in the same service both using the next number → whoever merges second renumbers before merging.
- Reference data the system needs (zones, rates, roles) = a normal versioned migration. Demo users and parcels = a separate seed script that runs only with the `local` profile.

### Table rules

- snake_case names; tables plural.
- Every table: `id uuid primary key`, `created_at timestamptz not null default now()`, `updated_at timestamptz`.
- Foreign keys **inside** your database: always declared. **No foreign keys to another service's data**: store the other ID as a plain `uuid` column (e.g. `parcels.customer_id`).
- Enum-like columns as `varchar` with a `check` constraint, mapped with `@Enumerated(EnumType.STRING)`.
- Money: `numeric(10,2)` + `char(3)` currency. Weights: `numeric(6,2)`.
- Index every column used for lookups or filters (`tracking_no`, `courier_id`, `status`, foreign keys).
- No personal data in columns that end up in logs or events (events carry IDs only).

### Example

```sql
-- services/parcel-service/src/main/resources/db/migration/V1__create_parcels.sql
create table parcels (
    id               uuid primary key,
    tracking_no      varchar(20)   not null unique,
    customer_id      uuid,                         -- user in auth_db, no FK
    merchant_id      uuid,                         -- merchant in merchant_db, no FK
    pickup_address   varchar(500)  not null,
    dropoff_address  varchar(500)  not null,
    recipient_name   varchar(150)  not null,
    recipient_phone  varchar(20)   not null,
    size             varchar(10)   not null check (size in ('SMALL','MEDIUM','LARGE')),
    weight_kg        numeric(6,2)  not null check (weight_kg > 0),
    fee_amount       numeric(10,2) not null,
    fee_currency     char(3)       not null default 'LKR',
    status           varchar(20)   not null default 'CREATED',
    created_at       timestamptz   not null default now(),
    updated_at       timestamptz
);

create index idx_parcels_customer on parcels (customer_id);
create index idx_parcels_merchant on parcels (merchant_id);
create index idx_parcels_status   on parcels (status);
```

### Local reset

Only for migrations that are **not** merged yet:

```bash
docker compose -f deploy/compose/docker-compose.yml down -v
docker compose -f deploy/compose/docker-compose.yml up -d postgres
```

Integration tests use Testcontainers, so every CI run applies all migrations to a fresh database.

## 4. Configuration

- `application.yml` uses placeholders: `spring.datasource.url: ${PARCEL_DB_URL:jdbc:postgresql://localhost:5432/parcel_db}`.
- Defaults only for non-secret local values. Secrets have no default.
- Env var names: `<SERVICE>_<THING>` in upper snake case, e.g. `PARCEL_DB_PASSWORD`, `AUTH_GOOGLE_CLIENT_ID`, `JWT_PUBLIC_KEY_URI`.
- Every variable listed in `deploy/compose/.env.example` with a dummy value and a comment.
- Profiles: `local` (default), `docker`, `k8s`, `test`.

## 5. Testing

| Kind | Tool | Naming |
|---|---|---|
| Unit | JUnit 5 + Mockito | `<Class>Test` |
| Web layer | `@WebMvcTest` + MockMvc + Spring Security test | `<Controller>Test` |
| Integration | `@SpringBootTest` + Testcontainers | `<Feature>IT` |
| Messaging | Testcontainers RabbitMQ + Awaitility | `<Flow>IT` |
| External HTTP | WireMock | inside the IT |
| Frontend | Vitest + React Testing Library | `<Component>.test.tsx` |

- Method names describe behaviour: `rejectsTransitionFromCreatedToDelivered()`.
- Every new endpoint: happy path, validation error, unauthenticated (401), wrong role or not owner (403).
- Required by the brief and owned by these people:
  - Event flow test (status change → message → consumer): Role 6 with Role 3.
  - Courier cannot access another courier's deliveries: Role 4.
- Coverage: JaCoCo per module, 70% line coverage minimum, enforced in CI. Role 9 aggregates the report.

## 6. Frontend

```
frontend/src
├── api/            typed API clients (one file per service), axios/fetch instance with auth
├── components/     shared UI (buttons, tables, status badges, layout)
├── features/
│   ├── auth/  booking/  tracking/  merchant/  courier/  dispatch/  admin/  assistant/
├── hooks/
├── routes/         route definitions, role guards
└── lib/            utils, formatting (LKR, dates)
```

- TypeScript strict; no `any` without a comment explaining why.
- Server state with TanStack Query; no ad-hoc `useEffect` fetching.
- Forms with react-hook-form + zod schemas that mirror the backend validation.
- Access token in memory; refresh token in an httpOnly cookie set by auth-service.
- Base URL from `VITE_API_BASE_URL`. No hard-coded hosts.
- Mobile-first: every page checked at 375 px width.
- Components in PascalCase, hooks `useSomething`, files match the export.

## 7. Docker

- Multi-stage Dockerfile per service: Maven build stage, then `eclipse-temurin:21-jre` runtime.
- Run as a non-root user. Expose only the service port.
- `HEALTHCHECK` or Compose `healthcheck` using `/actuator/health`.
- Image name `ghcr.io/<owner>/swiftdrop-<service>:<tag>`.
