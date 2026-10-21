# AGENTS.md

Instructions for AI coding agents (Claude Code, Codex, Copilot, Cursor, etc.) and for humans working in this repository. Read this file fully before making changes.

## 1. Project in one paragraph

SwiftDrop is a parcel delivery and courier management platform built as a university group project. It is a set of Spring Boot microservices behind a Spring Cloud Gateway, with a React frontend, PostgreSQL (one database per service), RabbitMQ for status events, Redis and MinIO. The architecture is fixed in `docs/architecture.md`; do not redesign it without a decision recorded in `docs/decisions.md`.

## 2. Repository map

| Path | What it is | Owner |
|---|---|---|
| `common/` | Shared library: ProblemDetail error handling, JWT resource-server config, event DTOs, PII log masking | Role 1 (team lead) |
| `services/api-gateway` | Spring Cloud Gateway, port 8080 | Role 1 |
| `services/pricing-service` | Zones, pricing rules, fee estimate, port 8082 | Role 1 |
| `services/auth-service` | Users, roles, JWT, Google OAuth 2.0, port 8081 | Role 2 |
| `services/parcel-service` | Parcels, status machine, history, receipts, SSE tracking, event publishing, port 8083 | Role 3 |
| `services/dispatch-service` | Assignments, courier day list, proof uploads, reports, port 8084 | Role 4 |
| `services/merchant-service` | Merchant registration/approval, API keys, merchant order API, port 8085 | Role 5 |
| `services/notification-service` | Event consumers, email/SMS, webhooks with retry, port 8086 | Role 6 |
| `services/ai-assistant` | Chatbot and MCP tools, port 8087 | Role 9 |
| `frontend/` | React + Vite + TypeScript app | Role 7 (+ Role 1) |
| `deploy/` | Docker Compose, Helm charts | Role 8 |
| `.github/workflows/` | CI pipelines | Role 8 |
| `docs/` | Design documentation | Everyone (Role 1 compiles) |

Stay inside the module you were asked to change. If a task needs a change in another owner's module, make the smallest change possible and say so in the PR description so that owner reviews it.

## 3. Tech stack and versions

- Java 21, Spring Boot 4.0.x, Spring Cloud 2025.1.x, Spring AI 2.x. Exact versions are pinned in the root `pom.xml`; never override them in a module.
- Maven multi-module build. Use the wrapper: `./mvnw`.
- PostgreSQL 16 + Flyway, RabbitMQ (Spring AMQP), Redis 7, MinIO (S3 API).
- Frontend: React 18, Vite, TypeScript (strict), Tailwind, React Router, TanStack Query, react-hook-form + zod, Recharts.
- Tests: JUnit 5, Mockito, Testcontainers, WireMock, Awaitility, JaCoCo; Vitest + React Testing Library.

## 4. Commands

```bash
./mvnw -q verify                                   # build + test everything
./mvnw -pl services/parcel-service -am verify      # one service and what it depends on
./mvnw -pl services/parcel-service spring-boot:run # run one service locally
./mvnw jacoco:report                               # coverage, see target/site/jacoco

docker compose -f deploy/compose/docker-compose.yml up -d postgres rabbitmq redis minio mailpit   # infra only
docker compose -f deploy/compose/docker-compose.yml up --build                                     # everything

cd frontend && npm ci && npm run dev               # frontend dev server
npm run lint && npm run test                       # frontend checks
```

Run the relevant tests before saying a task is done.

## 5. Non-negotiable rules

1. **Never commit secrets.** All secrets and environment-specific values come from environment variables. Add new variables to `deploy/compose/.env.example` with a dummy value and to the Helm values.
2. **Never log or return personal data.** No phone numbers, addresses, emails or tokens in logs, exceptions or error responses. Use the masking helpers in `common`.
3. **Errors go through the shared handler.** Throw domain exceptions from `common` (`NotFoundException`, `ConflictException`, `ForbiddenException`, `ValidationException`); never build error JSON by hand. Responses are RFC 7807 `ProblemDetail`.
4. **Validate every request DTO** with Bean Validation (`@Valid`, `@NotBlank`, `@Positive`, etc.).
5. **Only parcel-service changes parcel status.** Other services call `PATCH /internal/parcels/{id}/status`. Never duplicate the status transition rules.
6. **Only parcel-service publishes `ParcelStatusChanged`**, through its outbox. Event DTOs live in `common` and are versioned; do not change a field without bumping `version` and updating every consumer.
7. **Consumers must be idempotent.** Store processed `eventId`s and skip duplicates.
8. **Each service talks only to its own database.** No cross-database queries or shared tables; call the owning service's API instead.
9. **Authorization is checked in the service**, not only at the gateway: role checks with `@PreAuthorize`, ownership checks (courier owns the assignment, customer owns the parcel, merchant owns the order) in the service layer.
10. **Database changes are Flyway migrations** (`V<n>__<description>.sql`). Never edit a migration that has been merged to `dev`; add a new one.
11. **No new dependencies or infrastructure** without noting it in the PR description. No new services without a decision in `docs/decisions.md`.

## 6. Code conventions (summary)

Full detail in `docs/conventions.md`.

- Base package: `lk.swiftdrop.<service>` (e.g. `lk.swiftdrop.parcel`).
- Layering per feature: `controller` → `service` → `repository`, plus `dto`, `entity`, `mapper`, `event`, `config`. Controllers contain no business logic.
- DTOs are Java records. Entities are never returned from controllers.
- Constructor injection only (no field `@Autowired`).
- REST paths: `/api/v1/<resource>` in plural kebab-case; internal service-to-service endpoints under `/internal/...` and never routed by the gateway.
- IDs are UUIDs. Timestamps are `Instant` in UTC, serialized ISO-8601.
- Money is `BigDecimal` with 2 decimals, currency LKR.
- Configuration in `application.yml` uses `${ENV_VAR:default}` placeholders; defaults only for non-secrets.
- Frontend: feature folders under `src/features/<feature>`, API calls only through `src/api/` clients, no hard-coded URLs (use `VITE_API_BASE_URL`).

## 7. Tests expected with every change

- Unit tests for service-layer logic (Mockito).
- Controller tests with MockMvc for validation and authorization (including a forbidden case).
- Integration tests with Testcontainers when a change touches the database or RabbitMQ.
- Keep each module's line coverage at or above 70% (CI checks this).
- Test names describe behaviour: `courierCannotViewAnotherCouriersDelivery()`.

## 8. Git rules for agents

- Work on a branch created from `dev` (see `docs/git-workflow.md`). Never commit to `main` or `dev`, never force-push shared branches, never rewrite history that others have pulled.
- Commit messages follow Conventional Commits with the module as scope: `feat(parcel): add receipt PDF endpoint`.
- Keep commits small and focused; do not mix formatting changes with logic changes.
- Do not open PRs or push unless the human asked you to.

## 9. When unsure

Check `docs/architecture.md` and `docs/conventions.md` first. If the answer is not there, ask the human instead of guessing, and suggest adding the answer to the docs.
