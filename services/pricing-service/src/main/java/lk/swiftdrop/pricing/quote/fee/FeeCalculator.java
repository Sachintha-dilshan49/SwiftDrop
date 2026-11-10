package lk.swiftdrop.pricing.quote.fee;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies the fee components in their {@code @Order} (Spring sorts the injected list) and builds the breakdown:
 * route base fee, extra weight, size, speed, then the promo discount (docs/architecture.md §8).
 */
@Component
public class FeeCalculator {

    private final List<FeeComponent> components;

    public FeeCalculator(List<FeeComponent> components) {
        this.components = List.copyOf(components);
    }

    public FeeBreakdown calculate(FeeContext context) {
        FeeBreakdown.Builder breakdown = FeeBreakdown.builder();
        for (FeeComponent component : components) {
            component.apply(context, breakdown);
        }
        return breakdown.build();
    }
}
