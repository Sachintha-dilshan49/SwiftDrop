package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One row of a parcel list. Status history is left out; it is part of {@code GET /parcels/{id}}. */
public record ParcelSummaryResponse(
        UUID id,
        String trackingNo,
        ParcelStatus status,
        String pickupAddress,
        String dropoffAddress,
        String recipientName,
        String recipientPhone,
        ParcelSize size,
        BigDecimal weightKg,
        BigDecimal feeAmount,
        String feeCurrency,
        Instant createdAt,
        Instant updatedAt
) {

    public static ParcelSummaryResponse fromEntity(Parcel parcel) {
        return new ParcelSummaryResponse(
                parcel.getId(),
                parcel.getTrackingNo(),
                parcel.getStatus(),
                parcel.getPickupAddress(),
                parcel.getDropoffAddress(),
                parcel.getRecipientName(),
                parcel.getRecipientPhone(),
                parcel.getSize(),
                parcel.getWeightKg(),
                parcel.getFeeAmount(),
                parcel.getFeeCurrency(),
                parcel.getCreatedAt(),
                parcel.getUpdatedAt()
        );
    }
}
