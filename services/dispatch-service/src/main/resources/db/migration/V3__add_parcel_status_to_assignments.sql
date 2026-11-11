-- The parcel's status as last accepted by parcel-service for this assignment: CREATED when assigned, then each
-- status the courier moved it to. Lets the courier screen offer the next step; parcel-service still decides.
-- Nullable because assignments made before this migration have no value.
ALTER TABLE assignments
    ADD COLUMN parcel_status VARCHAR(32),
    ADD CONSTRAINT assignments_parcel_status_check CHECK (parcel_status IN
        ('CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'));
