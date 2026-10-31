package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;

import java.math.BigDecimal;
import java.util.UUID;

/** Reply to merchant-service: only what it stores with the order, no recipient details. */
public record CreateInternalParcelResponse(
        UUID id,
        String trackingNo,
        ParcelStatus status,
        BigDecimal feeAmount,
        String feeCurrency
) {

    public static CreateInternalParcelResponse fromEntity(Parcel parcel) {
        return new CreateInternalParcelResponse(parcel.getId(), parcel.getTrackingNo(), parcel.getStatus(),
                parcel.getFeeAmount(), parcel.getFeeCurrency());
    }
}
