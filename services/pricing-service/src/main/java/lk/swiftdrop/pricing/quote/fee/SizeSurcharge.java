package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** The size's surcharge. The weight must fit the size, and a size without an active rule is not offered. */
@Component
@Order(FeeOrder.SIZE)
public class SizeSurcharge implements FeeComponent {

    @Override
    public void apply(FeeContext context, FeeBreakdown.Builder breakdown) {
        ParcelSize size = context.size();
        PricingRules.SizeRule rule = context.rules().size(size)
                .orElseThrow(() -> new ValidationException("The " + size + " parcel size is not available right now.",
                        List.of(new FieldErrorDetail("size", "is not available"))));
        if (context.weightKg().compareTo(rule.maxWeightKg()) > 0) {
            String max = rule.maxWeightKg().stripTrailingZeros().toPlainString();
            throw new ValidationException("A " + size + " parcel can weigh at most " + max + " kg. Choose a larger size.",
                    List.of(new FieldErrorDetail("weightKg", "must be at most " + max + " kg for size " + size)));
        }
        breakdown.sizeFee(rule.surcharge());
    }
}
