# Team and Ownership

Roles confirmed 25 October 2026. GitHub usernames still to be added. Keep `.github/CODEOWNERS` in sync with this table.

## Roles

| # | Role | Member | GitHub | Owns | Demo part |
|---|---|---|---|---|---|
| 1 | Team lead, architecture, gateway, pricing (+ frontend support) | N.L. Sachintha Dilshan | @Sachintha-dilshan49 | `services/api-gateway`, `services/pricing-service`, `common/`, `docs/`, dispatcher/admin and pricing screens | Architecture walkthrough, fee estimate |
| 2 | Authentication and user management | Thulani | | `services/auth-service`, security config in `common/` | Google login, how the system is secured |
| 3 | Parcel and live tracking | Sanduni | | `services/parcel-service` | Creating a delivery, live tracking |
| 4 | Dispatch and courier | Tharuki | | `services/dispatch-service` | Dispatcher assigning, courier updating status |
| 5 | Merchant platform | Sudari | | `services/merchant-service` | Order through the API, merchant dashboard |
| 6 | Events, notifications and webhooks | Thisaru | | `services/notification-service`, RabbitMQ topology | Notification on status change, webhook with retry |
| 7 | Frontend and real-time UI | Supuni | | `frontend/` | UI flow, responsiveness |
| 8 | DevOps: Docker, Kubernetes, CI/CD | Kevinda | | `deploy/`, `.github/workflows/`, Dockerfiles | Running system, CI pipeline |
| 9 | AI assistant and testing coordinator | Devmith | | `services/ai-assistant`, shared test setup, coverage report | Chatbot, test and coverage results |

## Backup buddies (PR reviewers)

Each person reviews one teammate's PRs and can explain their part if needed.

| Author | Reviewer |
|---|---|
| Sachintha (Role 1) | Kevinda (Role 8) |
| Thulani (Role 2) | Sudari (Role 5) |
| Sanduni (Role 3) | Thisaru (Role 6) |
| Tharuki (Role 4) | Sanduni (Role 3) |
| Sudari (Role 5) | Thulani (Role 2) |
| Thisaru (Role 6) | Tharuki (Role 4) |
| Supuni (Role 7) | Sachintha (Role 1) |
| Kevinda (Role 8) | Devmith (Role 9) |
| Devmith (Role 9) | Supuni (Role 7) |

## Shared responsibilities (everyone)

- Tests for your own part (unit, integration, and authorization).
- Diagrams for your area in the design PDF (use case, sequence, ER part).
- Screens for your own feature, built with the frontend owner's components.
- One PR review cadence: within 24 hours.
- Updates in the WhatsApp group every Monday and Thursday: done / next / blocked.

## Cross-cutting owners

| Concern | Owner |
|---|---|
| API conventions and error format | Role 1 |
| Event contract (`ParcelStatusChanged`) | Role 3 publishes, Role 6 consumes; changes need both |
| Security and PII masking | Role 2 |
| Proof photo storage (MinIO) | Role 4, infra by Role 8 |
| Test setup, coverage report | Role 9 |
| README and contribution sheet | Role 8 and Role 1 |
| Design PDF compilation | Role 1 |
