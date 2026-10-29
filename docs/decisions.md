# Architecture Decision Log

Short records of decisions that shape the system. Add a new entry instead of editing an accepted one; mark old entries as superseded.

Format: **ID · title · status · date**, then context, decision, consequences.

---

## ADR-001 · Microservices behind an API gateway · Accepted · 2026-10-21

**Context.** Nine members, each needing a clear, demonstrable part. The brief marks integration patterns and offers a Kubernetes bonus.

**Decision.** Seven Spring Boot services plus a Spring Cloud Gateway. No service registry or config server; one PostgreSQL instance with a database per service; shared `common` library; Maven monorepo.

**Consequences.** Clear ownership and real integration boundaries. More moving parts to run locally, so Docker Compose must start everything with one command and services need timeouts on remote calls.

**Alternatives considered (2026-10-25).** The brief also allows a layered architecture or a modular monolith with a BFF. A modular monolith (one Spring Boot app, a schema per module, separate notification and AI services) would be simpler to run and debug. The team chose microservices because each member owns and demonstrates an independent service, the broker and webhook integration cross real service boundaries, and the Kubernetes bonus deploys several independent workloads. Reconfirmed by the team on 2026-10-25.

## ADR-002 · RabbitMQ for status events · Accepted · 2026-10-21

**Context.** Status changes must trigger notifications and merchant webhooks asynchronously, with retry.

**Decision.** RabbitMQ topic exchange `parcel.events`, one queue per consumer, retry via TTL queues and a dead-letter exchange, parking queue after three retries.

**Consequences.** Simple to run and demo (management UI). Kafka-style replay of history is not available; not needed for this scope.

## ADR-003 · Server-Sent Events for live tracking · Proposed · 2026-10-21

**Context.** Tracking updates flow only from server to browser.

**Decision.** SSE endpoint in parcel-service, routed through the gateway; the browser uses `EventSource`.

**Consequences.** Simpler than WebSockets, automatic reconnect, works through the gateway and ingress (with buffering off). No client-to-server messages, which we do not need.

## ADR-004 · API keys for merchant API access · Proposed · 2026-10-21

**Context.** The brief allows API keys or OAuth 2.0 client credentials.

**Decision.** Per-merchant API keys in the `X-API-Key` header, stored hashed, rate-limited per key.

**Consequences.** Easy for merchants to test with curl/Postman. OAuth 2.0 is still covered by Google login.

## ADR-005 · Transactional outbox in parcel-service · Proposed · 2026-10-21

**Context.** If RabbitMQ is down when a status changes, the event would be lost.

**Decision.** Write the event to `outbox_events` in the same transaction as the status change; a scheduled relay publishes and marks rows as sent.

**Consequences.** No lost events; extra table and relay job; consumers must be idempotent (they are anyway).

## ADR-006 · Custom auth-service instead of Keycloak · Proposed · 2026-10-21

**Context.** Keycloak would provide login, Google federation, roles and client credentials out of the box.

**Decision.** Build auth-service with Spring Security (BCrypt, RS256 JWT with JWKS, Google OAuth 2.0 client). Revisit only if auth-service falls behind schedule.

**Consequences.** The auth owner has real code to explain and test, which the demo and viva require. Lighter local setup (Keycloak needs ~1 GB RAM and its own database). More code to write and secure ourselves.

## ADR-007 · Spring Boot 4.0 line · Superseded by ADR-009 · 2026-10-21

**Context.** Spring Boot 4.0 is the current generation, with Spring Cloud 2025.1 and Spring AI 2.x built for it. Many tutorials still show Boot 3.

**Decision.** Use Spring Boot 4.0.x, Spring Cloud 2025.1.x, Spring AI 2.x, pinned in the root `pom.xml`.

**Consequences.** Supported versions and a single compatible set. Some starters and package names differ from older tutorials (e.g. Jackson 3, the Gateway server starter name); the team lead documents fixes in this repo as they come up.

## ADR-008 · Database per service, migrations reviewed by the team lead · Accepted · 2026-10-25

**Context.** The team considered one shared database with a single person writing all migrations, to keep the schema consistent.

**Decision.** Keep a database per service (one PostgreSQL server, separate databases and users). Each service owner writes their own Flyway migrations; the team lead reviews every migration through CODEOWNERS. No foreign keys or queries across service databases.

**Consequences.** Services stay independent, which is the point of ADR-001; a shared database would couple them and invite cross-service joins. Owners can explain their own tables in the viva. No single person blocks seven others. Consistency comes from `docs/conventions.md` §3 and the lead's review rather than from one author.

**Reconfirmed** by the team on 2026-10-29; one database with a schema per service was considered and rejected.

## ADR-009 · Spring Boot 4.1 line · Accepted · 2026-10-25

Supersedes ADR-007 (the version line only; the rest of ADR-007 still applies).

**Context.** When the Maven build was set up, Spring Boot 4.1.2 was the current release. Open-source support for Boot 4.0 ends on 31 December 2026, before the project ends. Spring AI 2.0.x and springdoc-openapi 3.2 are built against Boot 4.1. start.spring.io lists Spring Cloud 2025.1.4 and Spring AI 2.0.x as compatible with Boot 4.0 and 4.1.

**Decision.** Use Spring Boot 4.1.x, Spring Cloud 2025.1.x, Spring AI 2.0.x, Testcontainers 2.0.x, pinned in the root `pom.xml`. Code targets Java 21 (`maven.compiler.release=21`), so a newer local JDK still produces Java 21 bytecode.

**Consequences.** Supported until mid-2027 without a mid-project upgrade. Spring Cloud and Spring AI versions are unchanged from ADR-007. Boot 4 differences to know about (also in `docs/conventions.md`):

- Starters are modular: `spring-boot-starter-webmvc` (not `-web`), `spring-boot-starter-flyway` is needed for Flyway auto-configuration, test slices need `spring-boot-starter-webmvc-test`, and `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure`.
- Jackson 3: `tools.jackson.databind.json.JsonMapper`; annotations stay in `com.fasterxml.jackson.annotation`.
- Spring Security 7 adds a `FACTOR_BEARER` authority to JWT logins and a `resource_metadata` parameter to the `WWW-Authenticate` header.
- The gateway uses `spring-cloud-starter-gateway-server-webflux`.
- Testcontainers 2 modules are named `testcontainers-<module>` and containers moved to module packages, e.g. `org.testcontainers.postgresql.PostgreSQLContainer`.
