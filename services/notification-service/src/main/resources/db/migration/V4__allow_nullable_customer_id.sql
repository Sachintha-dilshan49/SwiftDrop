-- Allow customer_id to be NULL for merchant orders.
-- Architecture §6.3: merchant orders have no customer (the booking came via the merchant API key,
-- not from a registered customer account). The email and SMS recipient is the parcel recipient,
-- whose contacts come from parcel-service, not auth-service.
ALTER TABLE notifications
    ALTER COLUMN customer_id DROP NOT NULL;

-- The existing index on customer_id is still useful for customer booking queries,
-- so we keep it (partial index would avoid NULL entries, but a simple index is fine here).

