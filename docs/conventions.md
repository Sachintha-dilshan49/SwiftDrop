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

## 3. Database

- Flyway migrations in `src/main/resources/db/migration`, named `V1__create_parcels.sql`, `V2__add_status_history.sql`.
- Tables and columns in snake_case; tables plural.
- Every table has `id uuid primary key`, `created_at timestamptz not null`, `updated_at timestamptz`.
- Foreign keys and indexes declared explicitly. Index every column used in a `WHERE` on a hot path (e.g. `tracking_no`, `courier_id`).
- Never edit a migration that is already on `dev`.

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
