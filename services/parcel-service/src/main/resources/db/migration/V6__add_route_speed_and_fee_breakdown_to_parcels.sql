-- Route, speed and fee breakdown from pricing-service's quote (docs/architecture.md §8), the
-- recipient's email for merchant orders, and the EXTRA_LARGE size.

ALTER TABLE parcels
    ADD COLUMN pickup_zone     VARCHAR(30),
    ADD COLUMN dropoff_zone    VARCHAR(30),
    ADD COLUMN recipient_email VARCHAR(254),
    ADD COLUMN speed           VARCHAR(10),
    ADD COLUMN promo_code      VARCHAR(30),
    ADD COLUMN route_type      VARCHAR(20),
    ADD COLUMN base_fee        NUMERIC(10,2),
    ADD COLUMN weight_fee      NUMERIC(10,2),
    ADD COLUMN size_fee        NUMERIC(10,2),
    ADD COLUMN express_fee     NUMERIC(10,2),
    ADD COLUMN discount        NUMERIC(10,2);

-- No endpoint created parcels before V6, so the table is normally empty. Rows made by hand get
-- neutral values: the same unknown zone at both ends (so SAME_ZONE), standard speed, and the
-- whole fee as the base fee, so the breakdown still adds up to fee_amount.
UPDATE parcels
SET pickup_zone  = 'UNKNOWN',
    dropoff_zone = 'UNKNOWN',
    speed        = 'STANDARD',
    route_type   = 'SAME_ZONE',
    base_fee     = fee_amount,
    weight_fee   = 0,
    size_fee     = 0,
    express_fee  = 0,
    discount     = 0;

ALTER TABLE parcels
    ALTER COLUMN pickup_zone  SET NOT NULL,
    ALTER COLUMN dropoff_zone SET NOT NULL,
    ALTER COLUMN speed        SET NOT NULL,
    ALTER COLUMN route_type   SET NOT NULL,
    ALTER COLUMN base_fee     SET NOT NULL,
    ALTER COLUMN weight_fee   SET NOT NULL,
    ALTER COLUMN size_fee     SET NOT NULL,
    ALTER COLUMN express_fee  SET NOT NULL,
    ALTER COLUMN discount     SET NOT NULL,
    -- V1 declared the size check inline, so PostgreSQL named it parcels_size_check
    DROP CONSTRAINT parcels_size_check,
    ALTER COLUMN size TYPE VARCHAR(20),
    ADD CONSTRAINT chk_parcels_size
        CHECK (size IN ('SMALL', 'MEDIUM', 'LARGE', 'EXTRA_LARGE')),
    ADD CONSTRAINT chk_parcels_speed
        CHECK (speed IN ('EXPRESS', 'STANDARD', 'NEXT_DAY')),
    ADD CONSTRAINT chk_parcels_route_type
        CHECK (route_type IN ('SAME_ZONE', 'NEIGHBOURING', 'COLOMBO_SUBURBS', 'INTER_CITY')),
    -- Merchant orders email the recipient; customer bookings email the customer (auth-service)
    ADD CONSTRAINT chk_parcels_recipient_email
        CHECK (merchant_id IS NULL OR recipient_email IS NOT NULL);
