-- ParcelStatusChanged events already applied to merchant orders, so a redelivered event is skipped (AGENTS.md rule 7)
CREATE TABLE processed_events (
    id            UUID         PRIMARY KEY,
    event_id      UUID         NOT NULL,
    consumer_name VARCHAR(50)  NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_processed_events_event_consumer UNIQUE (event_id, consumer_name)
);
