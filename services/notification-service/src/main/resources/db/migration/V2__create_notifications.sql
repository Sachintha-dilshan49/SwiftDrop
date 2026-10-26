CREATE TABLE notifications (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       UUID          NOT NULL,
    parcel_id      UUID          NOT NULL,
    tracking_no    VARCHAR(20)   NOT NULL,
    customer_id    UUID          NOT NULL,
    type           VARCHAR(10)   NOT NULL,
    recipient      VARCHAR(255)  NOT NULL,
    subject        VARCHAR(500),
    body           TEXT          NOT NULL,
    status         VARCHAR(10)   NOT NULL,
    failure_reason TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ
);

CREATE INDEX idx_notifications_event_id    ON notifications(event_id);
CREATE INDEX idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX idx_notifications_parcel_id   ON notifications(parcel_id);
