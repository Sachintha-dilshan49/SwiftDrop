CREATE TABLE merchant_api_keys (
    id                  UUID PRIMARY KEY,
    merchant_id         UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    key_prefix          VARCHAR(20)  NOT NULL,
    key_hash            VARCHAR(64)  NOT NULL UNIQUE,
    revoked             BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at          TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_merchant ON merchant_api_keys (merchant_id);
CREATE INDEX idx_api_keys_hash ON merchant_api_keys (key_hash);
