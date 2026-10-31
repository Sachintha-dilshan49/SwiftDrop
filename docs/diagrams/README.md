# Diagrams

Export every diagram for the design PDF here (PNG or SVG plus the source file).  
All `.mmd` files can be pasted into [mermaid.live](https://mermaid.live) to render and export.

`.puml` files render with PlantUML ([plantuml.com](https://www.plantuml.com/plantuml) or the VS Code PlantUML extension). Diagrams split to fit A4 export one PNG per page: `name.png`, `name_001.png`, …

| File | Diagram | Owner |
|---|---|---|
| `usecase-notification.mmd` | Use case — notification-service actors and features | Role 6 |
| `arch-notification.mmd` | Architecture — notification-service internal structure and dependencies | Role 6 |
| `seq-notification-email-sms.mmd` | Sequence — email and SMS notification flow | Role 6 |
| `seq-webhook-retry.mmd` | Sequence — webhook delivery with retry chain | Role 6 |
| `seq-webhook-replay.mmd` | Sequence — admin replays a parked webhook | Role 6 |
| `seq-courier-status.mmd` | Sequence — courier status update triggers notifications (cross-role) | Role 4 + Role 6 |
| `seq-create-delivery.puml` | Sequence — customer creates a delivery (5 PNG pages, landscape) | Role 3 |
| `seq-live-tracking.puml` | Sequence — real-time parcel tracking over SSE (4 PNG pages, landscape) | Role 3 |
| `er-parcel-service.puml` | ER — parcel-service (`parcel_db`) | Role 3 |
| `state-parcel-status.puml` | State — parcel status lifecycle | Role 3 |
| `activity-parcel-status-update.puml` | Activity — parcel status update checks (2 PNG pages) | Role 3 |
| `seq-parcel-history-receipt.puml` | Sequence — delivery history, parcel details, receipt PDF and public lookup (4 PNG pages, landscape) | Role 3 |
| `seq-internal-parcel-api.puml` | Sequence — internal API: merchant orders and recipient contacts (4 PNG pages, landscape) | Role 3, contract with Role 5 + Role 6 |
| `arch-parcel-service.puml` | Component — parcel-service endpoints, parts and dependencies (landscape) | Role 3 |
| `customer/UC-00` … `UC-04` `.puml` | Use case — customer: overview, account, booking, tracking, AI assistant (PNG + SVG, descriptions in `customer/customer-use-cases.md`) | Role 7 |
| `er-` | ER diagram (full and per service) | Each service owner, merged by Role 1 |
| `arch-` | Architecture overview (full system) | Role 1 |
| `seq-merchant-order` | Merchant API order creation | Role 5 |
| `seq-google-login` | OAuth 2.0 login flow | Role 2 |
