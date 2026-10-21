# Team and Ownership

Fill in names and GitHub usernames once roles are confirmed. Keep `.github/CODEOWNERS` in sync with this table.

## Roles

| # | Role | Member | GitHub | Owns | Demo part |
|---|---|---|---|---|---|
| 1 | Team lead, architecture, gateway, pricing (+ frontend support) | N.L. Sachintha Dilshan | @Sachintha-dilshan49 | `services/api-gateway`, `services/pricing-service`, `common/`, `docs/`, dispatcher/admin and pricing screens | Architecture walkthrough, fee estimate |
| 2 | Authentication and user management | | | `services/auth-service`, security config in `common/` | Google login, how the system is secured |
| 3 | Parcel and live tracking | | | `services/parcel-service` | Creating a delivery, live tracking |
| 4 | Dispatch and courier | | | `services/dispatch-service` | Dispatcher assigning, courier updating status |
| 5 | Merchant platform | | | `services/merchant-service` | Order through the API, merchant dashboard |
| 6 | Events, notifications and webhooks | | | `services/notification-service`, RabbitMQ topology | Notification on status change, webhook with retry |
| 7 | Frontend and real-time UI | | | `frontend/` | UI flow, responsiveness |
| 8 | DevOps: Docker, Kubernetes, CI/CD | | | `deploy/`, `.github/workflows/`, Dockerfiles | Running system, CI pipeline |
| 9 | AI assistant and testing coordinator | | | `services/ai-assistant`, shared test setup, coverage report | Chatbot, test and coverage results |

## Backup buddies (PR reviewers)

Each person reviews one teammate's PRs and can explain their part if needed.

| Author | Reviewer |
|---|---|
| 1 Lead | 8 DevOps |
| 2 Auth | 5 Merchant |
| 3 Parcel | 6 Events |
| 4 Dispatch | 3 Parcel |
| 5 Merchant | 2 Auth |
| 6 Events | 4 Dispatch |
| 7 Frontend | 1 Lead |
| 8 DevOps | 9 AI/Test |
| 9 AI/Test | 7 Frontend |

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
