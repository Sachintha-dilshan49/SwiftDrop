package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What anyone with the tracking number may see (diagram seq-parcel-history-receipt, part 4): no parcel,
 * customer, merchant or courier IDs, no names, phone numbers or addresses, and no free-text notes or
 * failure reasons. It is also the SNAPSHOT event of the live tracking stream.
 */
public record PublicTrackingResponse(
        String trackingNo,
        ParcelStatus status,
        ParcelSize size,
        BigDecimal weightKg,
        Instant createdAt,
        List<PublicStatusHistoryResponse> history
) {

    public static PublicTrackingResponse fromEntity(Parcel parcel) {
        List<PublicStatusHistoryResponse> history = parcel.getStatusHistory().stream()
                .map(entry -> new PublicStatusHistoryResponse(entry.getStatus(), entry.getCreatedAt()))
                .toList();
        return new PublicTrackingResponse(parcel.getTrackingNo(), parcel.getStatus(), parcel.getSize(),
                parcel.getWeightKg(), parcel.getCreatedAt(), history);
    }
}
