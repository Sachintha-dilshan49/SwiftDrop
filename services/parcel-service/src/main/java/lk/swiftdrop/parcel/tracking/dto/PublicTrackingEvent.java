package lk.swiftdrop.parcel.tracking.dto;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;

import java.time.Instant;

/**
 * Status change pushed to public tracking pages over SSE. Only these four fields leave the
 * service: no parcel, customer, merchant or courier IDs, no notes and no failure reasons.
 */
public record PublicTrackingEvent(
        String trackingNo,
        ParcelStatus status,
        ParcelStatus previousStatus,
        Instant occurredAt
) {

    public static PublicTrackingEvent from(ParcelStatusChanged event) {
        return new PublicTrackingEvent(event.trackingNo(), event.toStatus(), event.fromStatus(), event.occurredAt());
    }
}
