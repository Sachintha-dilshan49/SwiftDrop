# SwiftDrop – Parcel Delivery & Courier Management System

SwiftDrop is an island-wide courier platform. Customers book and track parcels live, partner online stores create orders through an API and receive webhooks, dispatchers assign parcels to couriers, and couriers update delivery progress from their phones.

> Group assignment – Enterprise Application Development · 9 members

## Architecture at a glance

- **Frontend:** one React + Vite + TypeScript app for all roles
- **Edge:** Spring Cloud Gateway (single entry point, JWT check, routing, rate limits)
- **Services:** auth, pricing, parcel, dispatch, merchant, notification, ai-assistant (Spring Boot, Java 21)
- **Integration:** RabbitMQ topic exchange `parcel.events` → notifications, merchant webhooks (with retry), reports, live tracking
- **Data:** PostgreSQL (one database per service), Redis, MinIO
- **Run:** Docker Compose locally, Kubernetes + Helm for the bonus, GitHub Actions CI

Full details: [`docs/architecture.md`](docs/architecture.md)

## Repository layout

```
swiftdrop/
├── AGENTS.md                 # rules for AI coding agents (and humans) working in this repo
├── CLAUDE.md                 # Claude Code entry point (imports AGENTS.md)
├── CONTRIBUTING.md           # short version of the git workflow
├── pom.xml                   # Maven parent (versions pinned here)
├── common/                   # shared library: errors, security config, event DTOs, log masking
├── services/
│   ├── api-gateway/          # :8080
│   ├── auth-service/         # :8081
│   ├── pricing-service/      # :8082
│   ├── parcel-service/       # :8083
│   ├── dispatch-service/     # :8084
│   ├── merchant-service/     # :8085
│   ├── notification-service/ # :8086
│   └── ai-assistant/         # :8087
├── frontend/                 # React app
├── deploy/
│   ├── compose/              # docker-compose.yml, .env.example, init scripts
│   └── helm/                 # Kubernetes charts (bonus)
├── docs/                     # architecture, workflow, conventions, diagrams
└── .github/                  # CI workflows, PR and issue templates, CODEOWNERS
```

## Build and run a service

Needs JDK 21 or newer (code is compiled for Java 21). No Maven install needed; use the wrapper (`mvnw.cmd` on Windows).

```bash
./mvnw verify                                          # build + all tests + 70% coverage gate, every module
./mvnw -pl services/parcel-service -am verify          # one service and what it depends on (common)
./mvnw -pl services/parcel-service -am install -DskipTests && \
  ./mvnw -pl services/parcel-service spring-boot:run   # run one service on its port (8083)
curl http://localhost:8083/actuator/health             # {"status":"UP"}
```

Every service gets error handling, request IDs, JWT security and masked logging from `common` automatically. Versions are pinned in the root `pom.xml` (Spring Boot 4.1, ADR-009).

## Quick start with Docker Compose (once the compose file exists)

```bash
git clone https://github.com/Sachintha-dilshan49/SwiftDrop.git
cd SwiftDrop
cp deploy/compose/.env.example deploy/compose/.env   # fill in values, never commit .env
docker compose -f deploy/compose/docker-compose.yml up --build
```

| URL | What |
|---|---|
| http://localhost:3000 | Frontend |
| http://localhost:8080 | API gateway |
| http://localhost:8080/swagger-ui.html | API docs (aggregated) |
| http://localhost:15672 | RabbitMQ management |
| http://localhost:8025 | Mailpit (caught emails) |
| http://localhost:9001 | MinIO console |

## How we work

- Branch from `dev`, open a PR back to `dev`. Nobody pushes to `main` or `dev` directly.
- Commit messages follow Conventional Commits: `feat(parcel): add status history endpoint`.
- Every PR needs one approval (your backup buddy) and green CI.

Read [`docs/git-workflow.md`](docs/git-workflow.md) before your first commit.

## Documentation

| Doc | Purpose |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Services, communication, events, security, deployment |
| [docs/git-workflow.md](docs/git-workflow.md) | Branches, commits, PRs, reviews, releases |
| [docs/team-and-ownership.md](docs/team-and-ownership.md) | Roles, owners, backup buddies, demo parts |
| [docs/conventions.md](docs/conventions.md) | API, code, testing and config conventions |
| [docs/decisions.md](docs/decisions.md) | Architecture decision log |
