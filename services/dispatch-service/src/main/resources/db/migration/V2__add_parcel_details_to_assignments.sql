-- What the courier needs to find the parcel, copied from parcel-service when the dispatcher assigns it
-- (the courier cannot read the parcel there: only its booker, dispatchers and admins can).
-- Nullable because assignments made before this migration have no copy.
-- Personal data (address, recipient name): never logged and never put in events.
ALTER TABLE assignments
    ADD COLUMN tracking_no VARCHAR(20),
    ADD COLUMN pickup_address VARCHAR(500),
    ADD COLUMN dropoff_address VARCHAR(500),
    ADD COLUMN recipient_name VARCHAR(150),
    ADD COLUMN parcel_size VARCHAR(16),
    ADD COLUMN weight_kg NUMERIC(6, 2);
