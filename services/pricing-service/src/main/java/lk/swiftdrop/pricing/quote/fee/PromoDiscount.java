package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.entity.PromoCode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A promo code takes a fixed amount off, at most the fee before it, so the total is never below 0. An unknown
 * or inactive code gets the same answer, so customers cannot probe which codes exist.
 */
@Component
@Order(FeeOrder.PROMO)
public class PromoDiscount implements FeeComponent {

    @Override
    public void apply(FeeContext context, FeeBreakdown.Builder breakdown) {
        String code = context.promoCode();
        if (code == null) {
            return;
        }
        PromoCode promo = context.promo();
        if (promo == null || !promo.isActive()) {
            throw rejected("Promo code " + code + " is not valid.");
        }
        if (!promo.hasStartedAt(context.now())) {
            throw rejected("Promo code " + code + " is not valid yet.");
        }
        if (promo.hasEndedAt(context.now())) {
            throw rejected("Promo code " + code + " has expired.");
        }
        breakdown.discount(promo.getDiscountAmount().min(breakdown.subtotal()));
    }

    private static ValidationException rejected(String detail) {
        return new ValidationException(detail, List.of(new FieldErrorDetail("promoCode", "is not valid")));
    }
}
