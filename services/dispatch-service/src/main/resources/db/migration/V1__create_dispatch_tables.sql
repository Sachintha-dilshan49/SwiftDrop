CREATE TABLE IF NOT EXISTS couriers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT couriers_user_id_unique UNIQUE (user_id),
    CONSTRAINT couriers_status_check CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE'))
);

CREATE INDEX IF NOT EXISTS idx_couriers_user_id ON couriers (user_id);
CREATE INDEX IF NOT EXISTS idx_couriers_status ON couriers (status);

CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parcel_id UUID NOT NULL,
    courier_id UUID NOT NULL,
    assigned_by_user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_assignments_courier
        FOREIGN KEY (courier_id) REFERENCES couriers (id)
        ON DELETE RESTRICT,
    CONSTRAINT assignments_status_check CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_assignments_parcel_id ON assignments (parcel_id);
CREATE INDEX IF NOT EXISTS idx_assignments_courier_id ON assignments (courier_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON assignments (status);

CREATE TABLE IF NOT EXISTS delivery_proofs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_delivery_proofs_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_delivery_proofs_assignment_id ON delivery_proofs (assignment_id);

CREATE TABLE IF NOT EXISTS daily_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    courier_id UUID NOT NULL,
    assigned_count INTEGER NOT NULL DEFAULT 0,
    completed_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_daily_stats_courier
        FOREIGN KEY (courier_id) REFERENCES couriers (id)
        ON DELETE CASCADE,
    CONSTRAINT daily_stats_unique UNIQUE (date, courier_id)
);

CREATE INDEX IF NOT EXISTS idx_daily_stats_courier_id ON daily_stats (courier_id);
CREATE INDEX IF NOT EXISTS idx_daily_stats_date ON daily_stats (date);

CREATE TABLE IF NOT EXISTS processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL,
    source VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT processed_events_event_id_unique UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_event_id ON processed_events (event_id);
