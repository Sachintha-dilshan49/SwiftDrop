package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Everything the fee components need, already looked up, so each component is a pure function.
 *
 * @param promoCode the promo code the customer entered, upper case; null when none was entered
 * @param promo     the promo code found for {@code promoCode}; null when none was entered or it does not exist
 * @param now       the time promo validity is checked against
 */
public record FeeContext(
        ZoneRef pickup,
        ZoneRef dropoff,
        BigDecimal weightKg,
        ParcelSize size,
        DeliverySpeed speed,
        String promoCode,
        PromoCode promo,
        PricingRules rules,
        Instant now
) {
}
