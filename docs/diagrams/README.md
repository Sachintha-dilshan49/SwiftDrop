# Diagrams

Export every diagram for the design PDF here (PNG or SVG plus the source file).

- `.mmd` files are Mermaid. The report set lives in one folder per area (below) and is rendered with
  `bash docs/diagrams/render.sh` (all) or `bash docs/diagrams/render.sh usecase/uc-guest.mmd` (one file):
  `@mermaid-js/mermaid-cli`, `mermaid-config.json`, white background, scale 2, PNG next to the source.
  Flowchart-style diagrams use the ELK layout (`layout: elk` front matter). Any `.mmd` can also be pasted into [mermaid.live](https://mermaid.live).
- `.puml` files render with PlantUML ([plantuml.com](https://www.plantuml.com/plantuml) or the VS Code PlantUML extension). Diagrams split to fit A4 export one PNG per page: `name.png`, `name_001.png`, …
- When a teammate's diagram had to be corrected, the corrected version credits them in its header comment. Image diagrams that were recreated keep the original as `<name>-original.*`.

All diagrams are checked against [`architecture.md`](../architecture.md) v1.2.

## Report set — design report, 1 November 2026

| Area | File | Diagram | Author / based on |
|---|---|---|---|
| Use case | `usecase/uc-system-overview.mmd` | All actors and their main use cases | Role 1 |
| Use case | `usecase/uc-guest.mmd` | Guest: public tracking, live SSE, fee estimate, sign-up, merchant registration | Role 1 |
| Use case | `UC-00_customer_overview.puml` | Customer overview | Supuni (Role 7) |
| Use case | `UC-01_customer_account.puml` | Customer account and sign-in | Supuni (Role 7) |
| Use case | `usecase/uc-customer-booking.mmd` | Customer books a delivery | Recreated from Supuni's UC-02 (Role 7) |
| Use case | `usecase/uc-customer-tracking.mmd` | Customer tracking, history, receipts, notifications | Recreated from Supuni's UC-03 (Role 7) |
| Use case | `UC-04_customer_ai_assistant.puml` | Customer AI assistant | Supuni (Role 7) |
| Use case | `usecase/uc-merchant.mmd` | Merchant, merchant store server, admin approval | Based on Sudari's `usecase-merchant.md` (Role 5) |
| Use case | `usecase/uc-courier.mmd` | Courier day list and status updates | Role 1, Role 4 to review |
| Use case | `usecase/uc-dispatcher-admin.mmd` | Dispatcher and Admin (Admin generalises Dispatcher) | Role 1 |
| Use case | `usecase/uc-notification.mmd` | notification-service | Based on Thisaru's `usecase-notification.mmd` (Role 6) |
| Use case | `Auth-Use case diagram.png` | auth-service use cases for all roles | Thulani (Role 2) |
| ER | `er/er-overview.mmd` | Six databases, foreign keys vs logical references | Role 1 |
| ER | `er/er-auth-db.mmd` | `auth_db` | From Thulani's migrations (Role 2) |
| ER | `er/er-pricing-db.mmd` | `pricing_db` | Role 1 |
| ER | `er-parcel-service.puml` | `parcel_db` | Sanduni (Role 3) |
| ER | `er/er-dispatch-db.mmd` | `dispatch_db` (design, no migration yet) | Role 1, Role 4 to confirm |
| ER | `er/er-merchant-db.mmd` | `merchant_db` | Converted from Sudari's `er-merchant.md` (Role 5) |
| ER | `er/er-notification-db.mmd` | `notification_db` | From Thisaru's migrations (Role 6) |
| Architecture | `architecture/arch-system-overview.mmd` | System by layer: presentation, edge, business logic, integration, data | Role 1 |
| Architecture | `architecture/arch-layered-service.mmd` | Presentation → business logic → data inside one service | Role 1 |
| Architecture | `architecture/arch-deployment.mmd` | Deployment: Docker Compose (implemented), Kubernetes with Helm (planned), CI | Role 1 with Role 8 |
| Architecture | `architecture/class-pricing-strategy.mmd` | Class diagram — pricing fee components as a Strategy pattern (design) | Role 1 |
| Architecture | `arch-parcel-service.puml` | parcel-service components | Sanduni (Role 3) |
| Architecture | `architecture/arch-notification-service.mmd` | notification-service components | Based on Thisaru's `arch-notification.mmd` (Role 6) |
| Architecture | `Auth-Architecture diagram.png` | auth-service context | Thulani (Role 2) |
| Integration | `integration/int-communication-styles.mmd` | Gateway, request/reply, pub/sub, webhook, SSE | Role 1 |
| Integration | `integration/int-rabbitmq-topology.mmd` | Exchange, queues, retry and parking queues, idempotency | Role 1 with Role 6 |
| Sequence 1 | `seq-create-delivery.puml` | Customer creates a delivery (5 pages) | Sanduni (Role 3) |
| Sequence 2 | `sequence/seq-merchant-api-order.mmd` | Merchant API order | Based on Sudari's `seq-merchant-order.md` (Role 5) |
| Sequence 3 | `sequence/seq-courier-status-notification.mmd` | Courier status update → notifications | Based on Thisaru's `seq-courier-status.mmd` (Role 4 + 6) |
| Sequence 4 | `sequence/seq-oauth2-google-login.mmd` | OAuth 2.0 login with Google | Based on Thulani's `seq-google-login.mmd` (Role 2) |
| Sequence | `Login flow (emailmobile + password).png`, `Refresh token flow.png` | Password login, token refresh | Thulani (Role 2) |
| Sequence | `sequence/seq-internal-contact-lookup.mmd` | Internal contact lookup with `X-Internal-Token` | Recreated from Thulani's image (Role 2) |
| Sequence | `sequence/seq-notification-email-sms.mmd` | Email and SMS for one event | Based on Thisaru's `seq-notification-email-sms.mmd` (Role 6) |
| Sequence | `seq-webhook-retry.mmd`, `seq-webhook-replay.mmd` | Webhook retry, admin replay | Thisaru (Role 6) |
| Sequence | `seq-live-tracking.puml`, `seq-parcel-history-receipt.puml`, `seq-internal-parcel-api.puml` | Live tracking, history and receipt, internal parcel API | Sanduni (Role 3) |
| Flowchart | `flowchart/flow-booking.mmd` | Booking flow | Role 1 |
| Flowchart | `state-parcel-status.puml` | Parcel status lifecycle (state diagram) | Sanduni (Role 3) |
| Flowchart | `activity-parcel-status-update.puml` | Status update checks (2 pages) | Sanduni (Role 3) |
| Flowchart | `flowchart/flow-webhook-retry.mmd` | Webhook retry and replay | Role 1 with Role 6 |
| Flowchart | `flowchart/flow-login.mmd` | Login, Google login, refresh, logout | Role 1, Role 2 to review |
| Flowchart | `flowchart/flow-request-authorization.mmd` | How every request is authenticated and authorized | Role 1 |

## All files from teammates

| File | Diagram | Owner | Status |
|---|---|---|---|
| `usecase-notification.mmd` | Use case — notification-service actors and features | Role 6 | Superseded by `usecase/uc-notification.mmd` |
| `arch-notification.mmd` | Architecture — notification-service internal structure and dependencies | Role 6 | Superseded by `architecture/arch-notification-service.mmd` |
| `seq-notification-email-sms.mmd` | Sequence — email and SMS notification flow | Role 6 | Superseded by `sequence/seq-notification-email-sms.mmd` |
| `seq-webhook-retry.mmd` | Sequence — webhook delivery with retry chain | Role 6 | Current |
| `seq-webhook-replay.mmd` | Sequence — admin replays a parked webhook | Role 6 | Current |
| `seq-courier-status.mmd` | Sequence — courier status update triggers notifications (cross-role) | Role 4 + Role 6 | Superseded by `sequence/seq-courier-status-notification.mmd` |
| `seq-create-delivery.puml` | Sequence — customer creates a delivery (5 PNG pages, landscape) | Role 3 | Current |
| `seq-live-tracking.puml` | Sequence — real-time parcel tracking over SSE (4 PNG pages, landscape) | Role 3 | Current |
| `er-parcel-service.puml` | ER — parcel-service (`parcel_db`) | Role 3 | Current |
| `state-parcel-status.puml` | State — parcel status lifecycle | Role 3 | Current |
| `activity-parcel-status-update.puml` | Activity — parcel status update checks (2 PNG pages) | Role 3 | Current |
| `seq-parcel-history-receipt.puml` | Sequence — delivery history, parcel details, receipt PDF and public lookup (4 PNG pages, landscape) | Role 3 | Current |
| `seq-internal-parcel-api.puml` | Sequence — internal API: merchant orders and recipient contacts (4 PNG pages, landscape) | Role 3, contract with Role 5 + Role 6 | Current |
| `arch-parcel-service.puml` | Component — parcel-service endpoints, parts and dependencies (landscape) | Role 3 | Current |
| `UC-00_customer_overview.puml` | Use case — every customer-facing use case on one page (PNG + SVG) | Role 7 | Current |
| `UC-01_customer_account.puml` | Use case — customer sign up, log in, Google login, sessions, log out (PNG + SVG) | Role 7 | Current |
| `UC-02_customer_booking-original.puml` | Use case — customer books a delivery with the fee estimate (PNG + SVG) | Role 7 | Superseded by `usecase/uc-customer-booking.mmd` |
| `UC-03_customer_tracking-original.puml` | Use case — delivery history, parcel details, live and public tracking, receipts, notifications (PNG + SVG) | Role 7 | Superseded by `usecase/uc-customer-tracking.mmd` |
| `UC-04_customer_ai_assistant.puml` | Use case — customer AI assistant questions and guardrails (PNG + SVG) | Role 7 | Current |
| `customer-use-cases.md` | Customer actors, use case descriptions (UC-C01 – UC-C20) and main flows | Role 7 | UC-C08, C11, C13, C15 need the same text fixes as UC-02/03 |
| `_style.iuml` | Shared PlantUML style included by the `UC-*` diagrams | Role 7 | Current |
| `usecase-merchant.md` | Use case — merchant platform | Role 5 | Superseded by `usecase/uc-merchant.mmd` |
| `er-merchant.md` | ER — `merchant_db` | Role 5 | Current; rendered as `er/er-merchant-db.mmd` |
| `seq-merchant-order.md` | Sequence — merchant API order creation | Role 5 | Superseded by `sequence/seq-merchant-api-order.mmd` |
| `architecture-overview.html` | Architecture overview (21 Oct) | Role 1 | Out of date; superseded by `architecture/arch-system-overview.mmd` |
| `seq-google-login.mmd` | Sequence — Google login | Role 2 | Superseded by `sequence/seq-oauth2-google-login.mmd` (gateway, Spring default OAuth paths) |
| `Auth-Use case diagram.png` | Use case — auth-service | Role 2 | Current |
| `Auth-Architecture diagram.png` | Architecture — auth-service context | Role 2 | Current |
| `Login flow (emailmobile + password).png` | Sequence — password login | Role 2 | Current |
| `Refresh token flow.png` | Sequence — refresh token rotation | Role 2 | Current |
| `Google OAuth login flow-original.png` | Sequence — Google login | Role 2 | Superseded by `sequence/seq-oauth2-google-login.mmd`: browser bypassed the gateway |
| `Internal contact lookup flow-original.png` | Sequence — internal contact lookup | Role 2 | Superseded by `sequence/seq-internal-contact-lookup.mmd`: showed a bearer JWT instead of `X-Internal-Token` |
