# Diagrams

Export every diagram for the design PDF here (PNG or SVG plus the source file).

| File prefix | Diagram | Owner |
|---|---|---|
| `usecase-` | Use case diagrams per actor | Each role for its actor |
| `er-` | ER diagram (full and per service) | Each service owner, merged by Role 1 |
| `arch-` | Architecture overview | Role 1 |
| `seq-create-delivery` | Customer creates a delivery | Role 3 |
| `seq-merchant-order` | Merchant API order creation | Role 5 |
| `seq-courier-status` | Courier status update triggers notifications | Role 4 + Role 6 |
| `seq-google-login` | OAuth 2.0 login flow | Role 2 |

## Role 4 diagrams (dispatch and courier)

| Source | Diagram |
|---|---|
| `usecase-courier.mmd` | Courier use cases: today's list, status update, proof photo, failed with reason; ownership check |
| `usecase-dispatcher.mmd` | Dispatcher/admin use cases: unassigned parcels, assign, workload, reports |
| `er-dispatch.mmd` | `dispatch_db` tables, matching `V1__create_dispatch_tables.sql` |
| `seq-courier-status.mmd` | Courier status update → parcel-service → outbox → RabbitMQ fan-out (with Role 6) |

Export each `.mmd` to `.svg` next to it for the design PDF (or paste it into https://mermaid.live and download):

```bash
npx -p @mermaid-js/mermaid-cli mmdc -i docs/diagrams/<name>.mmd -o docs/diagrams/<name>.svg
```

`parcel_id`, `user_id` and `assigned_by_user_id` have no foreign keys on purpose: they point
to rows in `parcel_db` and `auth_db`, and each service talks only to its own database.
