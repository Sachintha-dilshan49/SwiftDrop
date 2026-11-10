CREATE TABLE parcel_status_history (
    id               UUID         PRIMARY KEY,
    parcel_id        UUID         NOT NULL REFERENCES parcels(id) ON DELETE CASCADE,
    status           VARCHAR(20)  NOT NULL,
    changed_by       UUID,
    changed_by_role  VARCHAR(50),
    note             VARCHAR(500),
    proof_reference  VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_status_history_parcel_id ON parcel_status_history (parcel_id);
CREATE INDEX idx_status_history_created_at ON parcel_status_history (created_at);
