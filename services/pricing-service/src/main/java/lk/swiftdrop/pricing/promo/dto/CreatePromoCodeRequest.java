package lk.swiftdrop.pricing.promo.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A new promo code, stored in upper case. The code cannot be changed later.
 *
 * @param validTo optional; null means the code does not expire, otherwise it must be after {@code validFrom}
 * @param active  optional, true when absent
 */
public record CreatePromoCodeRequest(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,30}$", message = "Code must be 1 to 30 letters, digits, - or _")
        String code,

        @NotNull(message = "Discount amount is required")
        @Positive(message = "Discount amount must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Discount amount must have at most 8 digits and 2 decimals")
        BigDecimal discountAmount,

        @NotNull(message = "Valid from is required")
        Instant validFrom,

        Instant validTo,

        Boolean active
) {
}
