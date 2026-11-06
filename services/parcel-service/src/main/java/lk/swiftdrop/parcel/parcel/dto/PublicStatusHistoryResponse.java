package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.common.event.ParcelStatus;

import java.time.Instant;

/** One public history entry: status and time only. Who made the change, notes and proof stay private. */
public record PublicStatusHistoryResponse(
        ParcelStatus status,
        Instant occurredAt
) {
}
