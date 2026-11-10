package lk.swiftdrop.dispatch.parcel;

import java.util.UUID;
import lk.swiftdrop.common.event.ParcelStatus;

/** Body of parcel-service's {@code PATCH /internal/parcels/{id}/status} ({@code UpdateParcelStatusRequest}). */
public record ParcelStatusUpdate(
        ParcelStatus status,
        UUID courierId,
        String note,
        String proofReference,
        String failureReason,
        UUID changedBy,
        String changedByRole) {

    /** parcel-service rejects a longer note. */
    static final int MAX_NOTE_LENGTH = 500;

    /**
     * A courier's update. parcel-service has no recipient field, so the recipient's name goes into the note.
     */
    public static ParcelStatusUpdate byCourier(ParcelStatus status, UUID courierId, UUID courierUserId, String note,
                                               String recipientName, String failureReason, String proofReference) {
        return new ParcelStatusUpdate(status, courierId, noteWithRecipient(note, recipientName), proofReference,
                failureReason, courierUserId, "COURIER");
    }

    private static String noteWithRecipient(String note, String recipientName) {
        boolean hasNote = note != null && !note.isBlank();
        if (recipientName == null || recipientName.isBlank()) {
            return hasNote ? note : null;
        }
        String combined = "Received by: " + recipientName.strip() + (hasNote ? ". " + note : "");
        return combined.length() <= MAX_NOTE_LENGTH ? combined : combined.substring(0, MAX_NOTE_LENGTH);
    }
}
