CREATE TABLE outbox_events (
    id          UUID         PRIMARY KEY,
    event_id    UUID         NOT NULL UNIQUE,
    event_type  VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload     TEXT         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
