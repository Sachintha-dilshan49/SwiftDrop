# Mock data

One `pg_dump` per SwiftDrop database with about a month of linked demo data, for local development and demos.
**Local use only.** Every account shares one test password, and nothing here is real.

| Database | What is in it |
|---|---|
| `auth_db` | 63 users: 2 admins, 3 dispatchers, 10 couriers, 8 merchants, 40 customers. Merchants keep the customer role from sign-up and got the merchant role when approved. Every courier has a different first name |
| `pricing_db` | The seeded zones and pricing rules, `DROP100`, and 4 more promo codes: `WELCOME50` (active), `AVURUDU2026` (expired), `XMAS250` (starts 14 Dec), `STAFFTEST` (switched off) |
| `parcel_db` | 250 parcels (170 booked by customers, 80 merchant orders) with 1,196 status history rows and their outbox events |
| `dispatch_db` | 10 couriers (one offline), 249 assignments, 120 proof-of-delivery records, daily stats |
| `merchant_db` | 8 merchants (6 approved, 1 pending, 1 rejected), API keys, webhook endpoints, 80 orders |
| `notification_db` | 1,658 email and SMS notifications, the processed events, 1,544 webhook delivery attempts |

Parcels run from 16 Oct to 14 Nov 2026, in Sri Lanka working hours. 211 were delivered and 20 failed. 19 are
still in progress on 14 Nov: booked and unassigned, assigned, picked up, in transit or out for delivery.
The rows in different databases match: each parcel's status, courier, tracking number and owner are the same in
parcel, dispatch, merchant and notification data.

## Accounts

The password for every account is `SwiftDrop@2026`.

| Role | Sign in with |
|---|---|
| Customer (30 parcels) | `customer@example.com` |
| Admin | `admin@swiftdrop.example.com`, `ops.admin@swiftdrop.example.com` |
| Dispatcher | `dispatcher1@swiftdrop.example.com` to `dispatcher3@…` |
| Courier | `courier01@swiftdrop.example.com` to `courier10@…` (`courier10` is offline) |

| Merchant | Status | Sign in with |
|---|---|---|
| BookNook Lanka | Approved | `chathura.perera@merchant.example.com` |
| Ceylon Tea Corner | Approved | `lahiru.wijesinghe@merchant.example.com` |
| Colombo Threads | Approved | `sajith.karunaratne@merchant.example.com` |
| Galle Fort Crafts | Approved | `pradeep.wickramasinghe@merchant.example.com` |
| Kandy Spice House | Approved | `chamari.peiris@merchant.example.com` |
| Negombo Fresh Fish | Pending | `pradeep.rajapaksa@merchant.example.com` |
| QuickFix Mobile Parts | Rejected | `oshada.weerasinghe@merchant.example.com` |
| TechLanka Gadgets | Approved | `lahiru.senanayake@merchant.example.com` |

**Demo staff accounts.** Without the dumps, set `AUTH_DEMO_ACCOUNTS_PASSWORD` in `deploy/compose/.env` and
auth-service creates `demo.admin@`, `demo.dispatcher@` and `demo.courier@swiftdrop.example.com` with that password.
Merchants are not seeded: register one in the app and approve it as the admin.

The other 39 customers have `firstname.lastname@example.com` addresses; list them with
`SELECT email FROM users ORDER BY email` in `auth_db`.

## Load the dumps

Start the stack once so Postgres creates the databases and users (`deploy/compose/postgres-init/init.sql`), then
from the repository root:

```bash
deploy/mock-data/load_dumps.sh
```

This **replaces** the data in all six databases. It stops the services while loading, empties the RabbitMQ
queues and starts the services again. The dumps include Flyway's history, so the services start without
migrating.

## Things to know

- **Dates are fixed.** "Today" in the data is 14 Nov 2026. After that day the courier's "today" list is
  empty. Regenerate to get fresh dates.
- **Proof photos are not in MinIO.** The proof records point at objects that only existed in the MinIO used
  to generate the data.
- **Webhooks all fail.** The mock merchants' webhook URLs are `example.com` addresses, so every delivery
  failed four times and was parked. The admin webhook screen shows those attempts.

## Regenerate

`generate_mock_data.py` creates the data through the services' own APIs, so every row passes their checks.
It uses SQL only where no API exists yet, and to move timelines into the past. Run it on fresh, migrated
databases with the whole stack running, then export:

```bash
deploy/mock-data/reset_databases.sh          # DELETES all data in the six databases, then restarts the services
python3 deploy/mock-data/generate_mock_data.py   # about 8 minutes, most of it waiting for webhook retries
deploy/mock-data/export_dumps.sh             # writes deploy/mock-data/dumps/*.sql
```

The generator works around gaps in the services. Each will be removed once the service handles it:

- Staff roles are written to `auth_db` directly, because the first admin cannot be created through the API.
- Couriers are inserted into dispatch's `couriers` table. dispatch registers a courier when they first open the
  courier screens, but the dispatcher needs them before that to assign parcels.

