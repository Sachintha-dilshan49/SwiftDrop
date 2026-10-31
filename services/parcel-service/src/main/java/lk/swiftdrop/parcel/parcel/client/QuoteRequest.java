package lk.swiftdrop.parcel.parcel.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;

import java.math.BigDecimal;

/** Body of {@code POST /internal/pricing/quote} (docs/architecture.md §8); {@code promoCode} is optional. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteRequest(
        String pickupZone,
        String dropoffZone,
        BigDecimal weightKg,
        ParcelSize size,
        DeliverySpeed speed,
        String promoCode
) {
}
