package lk.swiftdrop.pricing.quote.dto;

/** Regular expressions shared by the request DTOs; the same as parcel-service's, so a valid parcel can be priced. */
public final class ValidationPatterns {

    /** A zone code such as {@code COLOMBO_SOUTH}. */
    public static final String ZONE = "^[A-Z][A-Z_]{0,29}$";

    public static final String ZONE_MESSAGE = "must be a zone code such as COLOMBO_SOUTH";

    /** e.g. {@code DROP100}; empty is allowed and means no promo code. */
    public static final String PROMO_CODE = "^[A-Za-z0-9_-]{0,30}$";

    public static final String PROMO_CODE_MESSAGE = "Promo code must be 1 to 30 letters, digits, - or _";

    private ValidationPatterns() {
    }
}
