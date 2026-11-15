package lk.swiftdrop.ai.tools;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Turns parcel-service codes and timestamps into words a customer understands, so the model can
 * repeat tool results as they are instead of showing "OUT_FOR_DELIVERY" or "2026-11-14T06:15:58Z".
 */
final class FriendlyText {

    /** SwiftDrop delivers in Sri Lanka, so times are shown in Sri Lanka time. */
    static final ZoneId SRI_LANKA = ZoneId.of("Asia/Colombo");

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(SRI_LANKA);

    private record Status(String label, String meaning) {
    }

    private static final Map<String, Status> STATUSES = Map.of(
            "CREATED", new Status("Booked", "The booking is confirmed and the parcel is waiting for a courier to pick it up."),
            "PICKED_UP", new Status("Picked up", "A courier has collected the parcel."),
            "IN_TRANSIT", new Status("In transit", "The parcel is on its way to the delivery area."),
            "OUT_FOR_DELIVERY", new Status("Out for delivery", "A courier is bringing the parcel to the recipient today."),
            "DELIVERED", new Status("Delivered", "The parcel has been delivered."),
            "FAILED", new Status("Delivery failed", "The courier could not deliver the parcel. Contact SwiftDrop support for next steps."));

    private FriendlyText() {
    }

    /** "OUT_FOR_DELIVERY" → "Out for delivery"; an unknown code is tidied up rather than dropped. */
    static String statusLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Status status = STATUSES.get(code);
        return status != null ? status.label() : words(code);
    }

    /** A code as words: "EXTRA_LARGE" → "Extra large", "NEXT_DAY" → "Next day". */
    static String words(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String words = code.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** One sentence explaining the status, or {@code null} for an unknown code. */
    static String statusMeaning(String code) {
        Status status = code == null ? null : STATUSES.get(code);
        return status == null ? null : status.meaning();
    }

    /** "2026-11-14T06:15:58Z" → "14 Nov 2026, 11:45 am" in Sri Lanka time; text that is not a time is kept. */
    static String time(String isoInstant) {
        if (isoInstant == null || isoInstant.isBlank()) {
            return null;
        }
        try {
            return TIME.format(Instant.parse(isoInstant)).replace("AM", "am").replace("PM", "pm");
        } catch (DateTimeParseException e) {
            return isoInstant;
        }
    }
}
