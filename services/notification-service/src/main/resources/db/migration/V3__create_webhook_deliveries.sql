CREATE TABLE webhook_deliveries (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID          NOT NULL,
    parcel_id       UUID          NOT NULL,
    merchant_id     UUID          NOT NULL,
    webhook_url     VARCHAR(2000) NOT NULL,
    http_status     INTEGER,
    attempt_number  INTEGER       NOT NULL DEFAULT 1,
    status          VARCHAR(10)   NOT NULL,
    request_body    TEXT,
    response_body   TEXT,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_webhook_deliveries_event_id    ON webhook_deliveries(event_id);
CREATE INDEX idx_webhook_deliveries_merchant_id ON webhook_deliveries(merchant_id);
CREATE INDEX idx_webhook_deliveries_status      ON webhook_deliveries(status);
