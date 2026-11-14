# Merchant API questions

## Getting access
Register your business as a merchant. Your account starts as PENDING until an admin approves it.
Only an APPROVED merchant can create API keys and orders.

## API keys
Create API keys in the merchant dashboard. A key is shown only once, so copy it straight away.
Send it in the X-API-Key header. You can revoke a key at any time. Requests are rate-limited per key.

## Creating orders
Create orders with POST /api/v1/merchant-api/orders. Send an idempotency key with each order:
repeating a request with the same key does not create a second parcel, so retries are safe.
Fees follow the same pricing rules as customer bookings.

## Webhooks
Set a webhook URL and signing secret in the dashboard. SwiftDrop calls the URL on every status
change. Each request carries an X-SwiftDrop-Signature header (sha256 HMAC of the body with your
secret), an event id and a timestamp, so you can reject forged or replayed calls.

## Failed webhooks
If your endpoint fails, SwiftDrop retries after about 10 seconds, 1 minute and 5 minutes. After that
the delivery is parked, and a SwiftDrop dispatcher or admin can replay it.
