package lk.swiftdrop.common.error;

/**
 * One entry of the {@code errors} array in a validation problem. The rejected value is
 * deliberately not included, because it may be personal data.
 */
public record FieldErrorDetail(String field, String message) {
}
