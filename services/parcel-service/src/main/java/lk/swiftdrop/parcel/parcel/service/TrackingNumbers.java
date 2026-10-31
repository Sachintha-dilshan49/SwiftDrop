package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Format of tracking numbers such as {@code SD-7K3Q-9P2M} (no I or O, no 0 or 1). */
public final class TrackingNumbers {

    public static final Pattern FORMAT = Pattern.compile("^SD-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$");

    private TrackingNumbers() {
    }

    /**
     * Trims and upper-cases user input, then checks the format.
     *
     * @throws ValidationException 400 when the input is not a tracking number
     */
    public static String normalize(String input) {
        String trackingNo = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(trackingNo).matches()) {
            throw new ValidationException("Not a valid tracking number.",
                    List.of(new FieldErrorDetail("trackingNo", "must look like SD-XXXX-XXXX")));
        }
        return trackingNo;
    }
}
