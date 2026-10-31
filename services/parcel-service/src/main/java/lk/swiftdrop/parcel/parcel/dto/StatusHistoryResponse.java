package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.common.event.ParcelStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        ParcelStatus status,
        UUID changedBy,
        String changedByRole,
        String note,
        String proofReference,
        Instant createdAt
) {
}
