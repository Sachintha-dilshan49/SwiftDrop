package lk.swiftdrop.pricing.rule.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** The only parts of a rule an admin can change. */
public record UpdatePricingRuleRequest(
        @NotNull(message = "Amount is required")
        @PositiveOrZero(message = "Amount must be 0 or more")
        @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 digits and 2 decimals")
        BigDecimal amount,

        @NotNull(message = "Active is required")
        Boolean active
) {
}
