package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The speed's surcharge, reported as {@code expressFee} (only {@code EXPRESS} costs extra with the current
 * rules). A speed without an active rule is not offered.
 */
@Component
@Order(FeeOrder.SPEED)
public class SpeedSurcharge implements FeeComponent {

    @Override
    public void apply(FeeContext context, FeeBreakdown.Builder breakdown) {
        DeliverySpeed speed = context.speed();
        breakdown.expressFee(context.rules().speed(speed)
                .orElseThrow(() -> new ValidationException("The " + speed + " delivery speed is not available right now.",
                        List.of(new FieldErrorDetail("speed", "is not available")))));
    }
}
