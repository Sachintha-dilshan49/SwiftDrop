package lk.swiftdrop.dispatch.unassigned;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.dispatch.parcel.WaitingParcel;

public record UnassignedParcelResponse(
        UUID parcelId,
        String trackingNo,
        String pickupAddress,
        String dropoffAddress,
        String size,
        BigDecimal weightKg,
        Instant createdAt) {

    static UnassignedParcelResponse from(WaitingParcel parcel) {
        return new UnassignedParcelResponse(parcel.id(), parcel.trackingNo(), parcel.pickupAddress(),
                parcel.dropoffAddress(), parcel.size(), parcel.weightKg(), parcel.createdAt());
    }
}
