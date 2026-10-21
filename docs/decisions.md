# Architecture Decision Log

Short records of decisions that shape the system. Add a new entry instead of editing an accepted one; mark old entries as superseded.

Format: **ID · title · status · date**, then context, decision, consequences.

---

## ADR-001 · Microservices behind an API gateway · Accepted · 2026-10-21

**Context.** Nine members, each needing a clear, demonstrable part. The brief marks integration patterns and offers a Kubernetes bonus.

**Decision.** Seven Spring Boot services plus a Spring Cloud Gateway. No service registry or config server; one PostgreSQL instance with a database per service; shared `common` library; Maven monorepo.

**Consequences.** Clear ownership and real integration boundaries. More moving parts to run locally, so Docker Compose must start everything with one command and services need timeouts on remote calls.

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

## ADR-007 · Spring Boot 4.0 line · Proposed · 2026-10-21

**Context.** Spring Boot 4.0 is the current generation, with Spring Cloud 2025.1 and Spring AI 2.x built for it. Many tutorials still show Boot 3.

**Decision.** Use Spring Boot 4.0.x, Spring Cloud 2025.1.x, Spring AI 2.x, pinned in the root `pom.xml`.

**Consequences.** Supported versions and a single compatible set. Some starters and package names differ from older tutorials (e.g. Jackson 3, the Gateway server starter name); the team lead documents fixes in this repo as they come up.
