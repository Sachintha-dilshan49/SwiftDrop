package lk.swiftdrop.parcel.outbox.entity;

public enum OutboxStatus {
    /** Waiting to be published, or waiting for its next retry. Holds back later events of the same parcel. */
    PENDING,
    /** Confirmed by the broker. */
    SENT,
    /**
     * Never published and never retried automatically: the payload is unreadable, or (from the V5
     * backfill) a later event of the parcel had already been sent. Does not hold back later events,
     * so consumers see a gap rather than an out-of-order status. Never broker outages.
     */
    PARKED
}
