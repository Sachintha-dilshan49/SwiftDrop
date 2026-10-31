# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Claude Code specifics

- Read `docs/architecture.md` before working on anything that crosses services, events or security.
- Before editing, state which module you will touch and confirm it matches the task. Stay inside that module unless told otherwise.
- Prefer running the narrowest check that proves the change: `./mvnw -pl services/<name> -am verify` for a backend module, `npm run test` in `frontend/` for UI (once `frontend/package.json` exists).
- When you add an endpoint, also add or update: the request/response DTOs, validation, a MockMvc test (happy path + validation error + forbidden), and the endpoint table in `docs/architecture.md` if it is public.
- When you add an environment variable, update `deploy/compose/.env.example` and the Helm values in the same change.
- Write commit messages in Conventional Commits format and end them with the attribution lines your harness requires.
- Never print the contents of `.env` files or secrets into the conversation.

## Running tests

- The build needs JDK 21+. With an older JDK it fails with `release version 21 not supported`. On Windows use `mvnw.cmd` (or `./mvnw` from Git Bash).
- Test split (root `pom.xml`): `*Test` / `*Tests` run in Surefire (`test` phase); `*IT` run in Failsafe (`verify` phase) and use Testcontainers, so they need Docker running. Name new tests accordingly or they won't run.
- The JaCoCo 70% line-coverage `check` is bound to `verify`, so running a subset of tests with `verify` can fail the gate. Use `test` for a single unit test:
  ```bash
  ./mvnw -pl services/parcel-service -am test -Dtest=ParcelServiceTest -Dsurefire.failIfNoSpecifiedTests=false
  ./mvnw -pl services/parcel-service -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=OutboxRelayIT -Djacoco.skip=true
  ```
  `-am` also builds `common`, so `failIfNoSpecifiedTests=false` is needed.

## Big picture

- **`common` is a Spring Boot auto-configuration** (`META-INF/spring/...AutoConfiguration.imports`). Any servlet service that depends on it automatically gets the `ProblemDetail` exception handler, request-ID filter, JWT resource-server security, PII-masking log converters, and `X-Internal-Token` enforcement on `/internal/**`. Don't re-implement any of these in a service; extend `common` instead (Role 1 owns it).
- **Service-to-service calls** go to `/internal/**` with `X-Internal-Token = INTERNAL_API_TOKEN` (ADR-010). Build clients with `common`'s `InternalRestClientFactory`. Services refuse to start without the token. Spring tests get a token automatically. In MockMvc, call your own internal endpoints with `header("X-Internal-Token", InternalTokenTestContextCustomizerFactory.TEST_TOKEN)`.
- **Events:** parcel-service writes `ParcelStatusChanged` to an outbox table in the same transaction as the status change. `OutboxRelay` publishes it to the RabbitMQ topic exchange `parcel.events`. Consumers dedupe on `eventId` (see notification-service's `processed_events` table). The contract (`common/.../event/`) has a contract test in parcel-service.
- **Pricing:** parcel-service gets the binding fee from pricing-service's `POST /internal/pricing/quote` at booking time and stores the fee breakdown on the parcel. The pricing rules are in `docs/architecture.md` §8.
- **Implementation state (as of Oct 2026):** parcel-, merchant- and notification-service contain real code and migrations. api-gateway, auth, pricing, dispatch and ai-assistant are still application-class skeletons. `frontend/` has only merchant feature pages and no `package.json` yet, and `deploy/helm/` is empty. Check what exists before assuming an endpoint or client is already implemented.
- Service-specific design docs: `docs/merchant-platform.md`, `docs/notification-service.md`. Postman collections are in `docs/postman/`.
