CREATE TABLE merchant_orders (
    id                  UUID PRIMARY KEY,
    merchant_id         UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    parcel_id           UUID NOT NULL,
    tracking_no         VARCHAR(50)   NOT NULL,
    recipient_name      VARCHAR(150)  NOT NULL,
    recipient_phone     VARCHAR(30)   NOT NULL,
    dropoff_address     VARCHAR(500)  NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'CREATED',
    fee_amount          NUMERIC(10,2),
    fee_currency        CHAR(3)       NOT NULL DEFAULT 'LKR',
    idempotency_key     VARCHAR(100),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ
);

CREATE INDEX idx_merchant_orders_merchant ON merchant_orders (merchant_id);
CREATE INDEX idx_merchant_orders_tracking ON merchant_orders (tracking_no);
CREATE INDEX idx_merchant_orders_parcel ON merchant_orders (parcel_id);
CREATE INDEX idx_merchant_orders_idemp ON merchant_orders (merchant_id, idempotency_key);
