package lk.swiftdrop.pricing.quote.fee;

/**
 * One part of the fee (Strategy pattern, docs/architecture.md §11). {@link FeeCalculator} applies the
 * components in their {@code @Order}; a component can read what earlier ones set, e.g. the promo discount
 * reads the subtotal. A new surcharge is a new component; the calculator does not change.
 */
public interface FeeComponent {

    /**
     * Adds this component's part to the breakdown.
     *
     * @throws lk.swiftdrop.common.error.ValidationException when the request cannot be priced; the detail is
     *                                                       shown to the customer
     */
    void apply(FeeContext context, FeeBreakdown.Builder breakdown);
}
