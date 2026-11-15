# Role 6 — Notification Service (Thisaru Ramanayake)

## Overview
Role 6 owns the `notification-service` (port 8086), which handles all outbound communication triggered by parcel status changes: email/SMS notifications to customers and recipients, and signed HMAC webhook deliveries to merchant servers.

## What Happens in This Role

1. **Event consumption** — `StatusChangeNotificationConsumer` and `WebhookConsumer` listen on RabbitMQ (`notification.q` and `webhook.q`). Both consumers are **idempotent**: processed `eventId`s are stored in `processed_events` and duplicates are silently skipped.

2. **Email & SMS** — On every `ParcelStatusChanged` event, `NotificationService` selects the appropriate template, renders it, and dispatches via `EmailChannel` (Mailpit/SMTP) or `SmsChannel` (mock). Every attempt is recorded in the `notifications` table with status `SENT` or `FAILED`. `customer_id` is nullable to support merchant-originated orders that have no linked customer.

3. **Webhook delivery** — `WebhookConsumer` fetches the merchant's webhook config via an internal call to `merchant-service`, signs the JSON payload with HMAC-SHA256, and POSTs it to the merchant URL. The `X-SwiftDrop-Signature`, `X-Event-Id`, and `X-Timestamp` headers are included on every request.

4. **Retry queue chain** — Failed deliveries are re-queued with an incrementing `x-retry-count` header. The `__TypeId__` header is preserved on each hop so Spring AMQP can deserialise the message on attempt 2–4. Routing: attempt 1 → `webhook.retry.10s`, attempt 2 → `webhook.retry.1m`, attempt 3 → `webhook.retry.5m`, attempt 4 → `webhook.parking`.

5. **Parking & admin replay** — Exhausted deliveries (attempt 4) are saved as `PARKED` in `webhook_deliveries`. An admin can list them via `GET /api/v1/admin/webhook-deliveries?status=PARKED` and trigger a replay via `POST /api/v1/admin/webhook-deliveries/{id}/replay`.

6. **Data layer** — `notification_db` holds two tables: `notifications` (email/SMS records) and `webhook_deliveries` (webhook attempt records with statuses `SUCCESS`, `FAILED`, `PARKED`). Schema is managed with Flyway; no foreign keys to other services' databases.
