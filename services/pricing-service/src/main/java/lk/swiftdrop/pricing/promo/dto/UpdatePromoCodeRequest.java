package lk.swiftdrop.pricing.promo.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Replaces everything about a promo code except the code itself.
 *
 * @param validTo optional; null means the code does not expire, otherwise it must be after {@code validFrom}
 */
public record UpdatePromoCodeRequest(
        @NotNull(message = "Discount amount is required")
        @Positive(message = "Discount amount must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Discount amount must have at most 8 digits and 2 decimals")
        BigDecimal discountAmount,

        @NotNull(message = "Valid from is required")
        Instant validFrom,

        Instant validTo,

        @NotNull(message = "Active is required")
        Boolean active
) {
}
