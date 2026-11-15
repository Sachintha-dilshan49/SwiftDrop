package lk.swiftdrop.dispatch.assignment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Copy of the parcel's delivery details, taken from parcel-service at assignment time. */
@Embeddable
public record ParcelDetails(
        @Column(name = "tracking_no", length = 20) String trackingNo,
        @Column(name = "pickup_address", length = 500) String pickupAddress,
        @Column(name = "dropoff_address", length = 500) String dropoffAddress,
        @Column(name = "recipient_name", length = 150) String recipientName,
        @Column(name = "parcel_size", length = 16) String size,
        @Column(name = "weight_kg", precision = 6, scale = 2) BigDecimal weightKg) {

    @Override
    public String toString() {
        // addresses and recipient name are personal data; keep them out of logs
        return "ParcelDetails[trackingNo=" + trackingNo + "]";
    }
}
