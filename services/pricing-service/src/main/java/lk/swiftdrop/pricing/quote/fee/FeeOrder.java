package lk.swiftdrop.pricing.quote.fee;

/** The order the fee components run in, as in the formula of docs/architecture.md §8. */
final class FeeOrder {

    static final int ROUTE_BASE = 100;
    static final int EXTRA_WEIGHT = 200;
    static final int SIZE = 300;
    static final int SPEED = 400;
    /** Last: the discount is capped at everything before it. */
    static final int PROMO = 500;

    private FeeOrder() {
    }
}
