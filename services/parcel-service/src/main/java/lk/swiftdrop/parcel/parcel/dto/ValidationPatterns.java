package lk.swiftdrop.parcel.parcel.dto;

/** Regular expressions shared by the request DTOs. */
public final class ValidationPatterns {

    /** 9 to 15 digits with an optional leading {@code +}, e.g. {@code +94771234567} or {@code 0771234567}. */
    public static final String PHONE = "^\\+?[0-9]{9,15}$";

    public static final String PHONE_MESSAGE = "Recipient phone must be 9 to 15 digits, optionally starting with +";

    /** A zone code from pricing-service, e.g. {@code COLOMBO_SOUTH}; pricing decides whether it exists. */
    public static final String ZONE = "^[A-Z][A-Z_]{0,29}$";

    public static final String ZONE_MESSAGE = "must be a zone code such as COLOMBO_SOUTH";

    /** e.g. {@code DROP100}; pricing decides whether the code is valid. */
    public static final String PROMO_CODE = "^[A-Za-z0-9_-]{1,30}$";

    public static final String PROMO_CODE_MESSAGE = "Promo code must be 1 to 30 letters, digits, - or _";

    private ValidationPatterns() {
    }
}
