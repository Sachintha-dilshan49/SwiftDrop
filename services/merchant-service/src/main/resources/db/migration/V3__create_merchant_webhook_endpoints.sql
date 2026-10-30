CREATE TABLE merchant_webhook_endpoints (
    id                  UUID PRIMARY KEY,
    merchant_id         UUID NOT NULL UNIQUE REFERENCES merchants(id) ON DELETE CASCADE,
    webhook_url         VARCHAR(1000) NOT NULL,
    secret              VARCHAR(255)  NOT NULL,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ
);

CREATE INDEX idx_webhooks_merchant ON merchant_webhook_endpoints (merchant_id);
