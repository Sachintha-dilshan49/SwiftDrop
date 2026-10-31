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
| `UC-00_customer_overview.puml` | Use case — every customer-facing use case on one page (PNG + SVG) | Role 7 |
| `UC-01_customer_account.puml` | Use case — customer sign up, log in, Google login, sessions, log out (PNG + SVG) | Role 7 |
| `UC-02_customer_booking.puml` | Use case — customer books a delivery with the fee estimate (PNG + SVG) | Role 7 |
| `UC-03_customer_tracking.puml` | Use case — delivery history, parcel details, live and public tracking, receipts, notifications (PNG + SVG) | Role 7 |
| `UC-04_customer_ai_assistant.puml` | Use case — customer AI assistant questions and guardrails (PNG + SVG) | Role 7 |
| `customer-use-cases.md` | Customer actors, use case descriptions (UC-C01 – UC-C20) and main flows | Role 7 |
| `_style.iuml` | Shared PlantUML style included by the `UC-*` diagrams | Role 7 |
| `er-` | ER diagram (full and per service) | Each service owner, merged by Role 1 |
| `arch-` | Architecture overview (full system) | Role 1 |
| `seq-merchant-order` | Merchant API order creation | Role 5 |
| `seq-google-login` | OAuth 2.0 login flow | Role 2 |
