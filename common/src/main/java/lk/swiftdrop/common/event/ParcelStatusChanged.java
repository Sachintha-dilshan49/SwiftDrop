package lk.swiftdrop.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code ParcelStatusChanged} v1, published only by parcel-service through its outbox
 * (docs/architecture.md §6). IDs only: no names, phone numbers or addresses.
 *
 * <p>Changing a field is a breaking change: bump {@link #VERSION} and update every consumer.
 *
 * @param eventId       unique per event; consumers store it to skip duplicates
 * @param merchantId    {@code null} for customer bookings
 * @param courierId     {@code null} before assignment
 * @param fromStatus    {@code null} for the first event ({@code CREATED})
 * @param note          optional free text from the courier or dispatcher
 * @param failureReason set when {@code toStatus} is {@code FAILED}
 */
public record ParcelStatusChanged(
        UUID eventId,
        String type,
        int version,
        Instant occurredAt,
        UUID parcelId,
        String trackingNo,
        UUID customerId,
        UUID merchantId,
        UUID courierId,
        ParcelStatus fromStatus,
        ParcelStatus toStatus,
        String note,
        String failureReason) {

    public static final String TYPE = "ParcelStatusChanged";
    public static final int VERSION = 1;

    public ParcelStatusChanged {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(parcelId, "parcelId");
        Objects.requireNonNull(trackingNo, "trackingNo");
        Objects.requireNonNull(toStatus, "toStatus");
    }

    /** New v1 event with a random {@code eventId}. */
    public static ParcelStatusChanged of(Instant occurredAt, UUID parcelId, String trackingNo, UUID customerId,
            UUID merchantId, UUID courierId, ParcelStatus fromStatus, ParcelStatus toStatus, String note,
            String failureReason) {
        return new ParcelStatusChanged(UUID.randomUUID(), TYPE, VERSION, occurredAt, parcelId, trackingNo,
                customerId, merchantId, courierId, fromStatus, toStatus, note, failureReason);
    }
}
