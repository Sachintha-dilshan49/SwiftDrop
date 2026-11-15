# Role 3: Parcel Service and Live Tracking (Sanduni)

Role 3 owns `parcel-service` (port 8083): every parcel, its status, its history, its receipt and its live tracking. It is the only service that changes a parcel's status.

## What happens in this role

1. **Booking.** `POST /api/v1/parcels` creates a parcel for the signed-in customer. The fee is never calculated here: parcel-service asks pricing-service for a binding quote (`POST /internal/pricing/quote`) and stores the full breakdown; if pricing is down, no parcel is created (503). Each parcel gets a tracking number like `SD-7KQ2-M9XA`. Merchant orders arrive the same way through `POST /internal/parcels` from merchant-service.
2. **My deliveries and receipts.** Customers list their own parcels (paged, filter by status), open one, and download a PDF receipt (`/{id}/receipt`, OpenPDF). Someone else's parcel answers 404, so parcel IDs cannot be guessed. Dispatchers and admins see all parcels.
3. **Status machine.** `CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED` or `FAILED` (a reason is required). Other services change status only through `PATCH /internal/parcels/{id}/status` with the internal token. The parcel row is locked during a change, so two updates at once cannot skip a step. Every change adds a row to `parcel_status_history`.
4. **Courier check.** The courier's first update stores their **auth user ID** as `courierId`; every later update must come from that same user, or it gets 403. Dispatch sends the user ID, not its own `couriers.id`. `CourierIdContractIT` locks this contract (with dispatch's `CourierStatusUpdateIT`).
5. **Events (outbox).** Each status change writes a `ParcelStatusChanged` event to `outbox_events` in the same transaction. A scheduled relay publishes it to the RabbitMQ exchange `parcel.events` (`parcel.status.<status>`), in commit order and retried on failure. Notifications, webhooks and live tracking all consume these events.
6. **Public and live tracking.** `GET /api/v1/track/{trackingNo}` shows a parcel's public view without login. `/stream` is an SSE stream: parcel-service consumes its own events and pushes each new status to open browsers, with a heartbeat to keep the connection alive.
7. **Data.** `parcel_db`: `parcels`, `parcel_status_history`, `outbox_events`, `processed_events` (duplicate events are skipped), Flyway V1–V6. No links to other services' databases.
