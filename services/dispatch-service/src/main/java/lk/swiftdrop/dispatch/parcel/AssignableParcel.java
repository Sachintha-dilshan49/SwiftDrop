package lk.swiftdrop.dispatch.parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
import lk.swiftdrop.common.event.ParcelStatus;

/**
 * A parcel from {@code GET /api/v1/parcels/{id}}: its status, to check it still waits for a courier, and what the
 * courier needs to deliver it. The recipient's phone and the fee are not read.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssignableParcel(
        UUID id,
        String trackingNo,
        ParcelStatus status,
        String pickupAddress,
        String dropoffAddress,
        String recipientName,
        String size,
        BigDecimal weightKg) {

    @Override
    public String toString() {
        // addresses and recipient name are personal data; keep them out of logs
        return "AssignableParcel[id=" + id + ", status=" + status + "]";
    }
}
