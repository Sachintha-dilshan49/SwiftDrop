# Diagrams

Export every diagram for the design PDF here (PNG or SVG plus the source file).  
All `.mmd` files can be pasted into [mermaid.live](https://mermaid.live) to render and export.

| File | Diagram | Owner |
|---|---|---|
| `usecase-notification.mmd` | Use case — notification-service actors and features | Role 6 |
| `arch-notification.mmd` | Architecture — notification-service internal structure and dependencies | Role 6 |
| `seq-notification-email-sms.mmd` | Sequence — email and SMS notification flow | Role 6 |
| `seq-webhook-retry.mmd` | Sequence — webhook delivery with retry chain | Role 6 |
| `seq-webhook-replay.mmd` | Sequence — admin replays a parked webhook | Role 6 |
| `seq-courier-status.mmd` | Sequence — courier status update triggers notifications (cross-role) | Role 4 + Role 6 |
| `er-` | ER diagram (full and per service) | Each service owner, merged by Role 1 |
| `arch-` | Architecture overview (full system) | Role 1 |
| `seq-create-delivery` | Customer creates a delivery | Role 3 |
| `seq-merchant-order` | Merchant API order creation | Role 5 |
| `seq-google-login` | OAuth 2.0 login flow | Role 2 |
