package lk.swiftdrop.ai.tools.dto;

import java.math.BigDecimal;

/** What {@code estimate_fee} returns to the model. The fee is an estimate; booking gets the binding quote. */
public record FeeEstimate(
        String pickupZone,
        String dropoffZone,
        BigDecimal weightKg,
        String size,
        String speed,
        BigDecimal amount,
        String currency) {
}
