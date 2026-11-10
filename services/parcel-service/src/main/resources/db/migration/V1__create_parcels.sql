CREATE TABLE parcels (
    id               UUID          PRIMARY KEY,
    tracking_no      VARCHAR(20)   NOT NULL UNIQUE,
    customer_id      UUID,
    merchant_id      UUID,
    courier_id       UUID,
    pickup_address   VARCHAR(500)  NOT NULL,
    dropoff_address  VARCHAR(500)  NOT NULL,
    recipient_name   VARCHAR(150)  NOT NULL,
    recipient_phone  VARCHAR(20)   NOT NULL,
    size             VARCHAR(10)   NOT NULL CHECK (size IN ('SMALL', 'MEDIUM', 'LARGE')),
    weight_kg        NUMERIC(6,2)  NOT NULL CHECK (weight_kg > 0),
    fee_amount       NUMERIC(10,2) NOT NULL,
    fee_currency     VARCHAR(3)    NOT NULL DEFAULT 'LKR',
    status           VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    failure_reason   VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ
);

CREATE INDEX idx_parcels_customer    ON parcels (customer_id);
CREATE INDEX idx_parcels_merchant    ON parcels (merchant_id);
CREATE INDEX idx_parcels_courier     ON parcels (courier_id);
CREATE INDEX idx_parcels_tracking_no ON parcels (tracking_no);
CREATE INDEX idx_parcels_status      ON parcels (status);
