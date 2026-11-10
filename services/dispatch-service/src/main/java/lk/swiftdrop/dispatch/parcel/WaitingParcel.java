package lk.swiftdrop.dispatch.parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A CREATED parcel from parcel-service's list. Only what a dispatcher needs to choose a courier; the recipient's
 * name and phone in parcel-service's reply are not read.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WaitingParcel(
        UUID id,
        String trackingNo,
        String pickupAddress,
        String dropoffAddress,
        String size,
        BigDecimal weightKg,
        Instant createdAt) {
}
