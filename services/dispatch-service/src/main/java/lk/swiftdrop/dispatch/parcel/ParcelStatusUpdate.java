package lk.swiftdrop.dispatch.parcel;

import java.util.UUID;
import lk.swiftdrop.common.event.ParcelStatus;

/** Body of parcel-service's {@code PATCH /internal/parcels/{id}/status}. */
public record ParcelStatusUpdate(
        ParcelStatus status,
        String note,
        String recipientName,
        String failureReason,
        UUID courierId,
        String proofObjectKey) {
}
