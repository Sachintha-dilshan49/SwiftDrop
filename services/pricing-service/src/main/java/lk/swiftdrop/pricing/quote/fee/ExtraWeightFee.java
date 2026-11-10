package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The first kg is included; every started kg after it costs the {@code EXTRA_KG} amount, e.g. 1.2 kg is one
 * extra kg. No parcel may weigh more than {@value #MAX_WEIGHT_KG} kg.
 */
@Component
@Order(FeeOrder.EXTRA_WEIGHT)
public class ExtraWeightFee implements FeeComponent {

    static final int MAX_WEIGHT_KG = 40;

    private static final BigDecimal MAX_WEIGHT = BigDecimal.valueOf(MAX_WEIGHT_KG);
    private static final BigDecimal INCLUDED_WEIGHT = BigDecimal.ONE;

    @Override
    public void apply(FeeContext context, FeeBreakdown.Builder breakdown) {
        BigDecimal weight = context.weightKg();
        if (weight.signum() <= 0) {
            throw new ValidationException("The weight must be greater than 0 kg.",
                    List.of(new FieldErrorDetail("weightKg", "must be greater than 0")));
        }
        if (weight.compareTo(MAX_WEIGHT) > 0) {
            throw new ValidationException("Parcels heavier than " + MAX_WEIGHT_KG + " kg cannot be delivered.",
                    List.of(new FieldErrorDetail("weightKg", "must be at most " + MAX_WEIGHT_KG + " kg")));
        }
        BigDecimal extraKg = weight.subtract(INCLUDED_WEIGHT).max(BigDecimal.ZERO).setScale(0, RoundingMode.CEILING);
        breakdown.weightFee(context.rules().extraKgFee().multiply(extraKg));
    }
}
