package lk.swiftdrop.pricing.quote.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;

import java.math.BigDecimal;

/**
 * What to price: the body of {@code POST /internal/pricing/quote} and the query of
 * {@code GET /api/v1/pricing/estimate}. The fields match parcel-service's {@code QuoteRequest}.
 *
 * <p>The 40 kg limit and the size limits are checked by the fee components, so the customer gets a clear
 * reason instead of a generic validation error.
 *
 * @param promoCode optional; letters are matched without regard to case, and blank means no promo code
 */
public record QuoteRequest(
        @NotBlank(message = "Pickup zone is required")
        @Pattern(regexp = ValidationPatterns.ZONE, message = "Pickup zone " + ValidationPatterns.ZONE_MESSAGE)
        String pickupZone,

        @NotBlank(message = "Drop-off zone is required")
        @Pattern(regexp = ValidationPatterns.ZONE, message = "Drop-off zone " + ValidationPatterns.ZONE_MESSAGE)
        String dropoffZone,

        @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be greater than 0")
        @Digits(integer = 4, fraction = 2, message = "Weight must be at most 9999.99 kg with up to 2 decimals")
        BigDecimal weightKg,

        @NotNull(message = "Size is required")
        ParcelSize size,

        @NotNull(message = "Speed is required")
        DeliverySpeed speed,

        @Pattern(regexp = ValidationPatterns.PROMO_CODE, message = ValidationPatterns.PROMO_CODE_MESSAGE)
        String promoCode
) {
}
