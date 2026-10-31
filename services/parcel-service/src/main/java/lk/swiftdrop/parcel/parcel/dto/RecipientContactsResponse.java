package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.parcel.parcel.entity.Parcel;

/**
 * Reply of {@code GET /internal/parcels/{id}/contacts} for notification-service. Personal data: it is
 * never logged and never put into events. {@code recipientEmail} is null for customer bookings.
 */
public record RecipientContactsResponse(
        String recipientName,
        String recipientPhone,
        String recipientEmail
) {

    public static RecipientContactsResponse fromEntity(Parcel parcel) {
        return new RecipientContactsResponse(parcel.getRecipientName(), parcel.getRecipientPhone(),
                parcel.getRecipientEmail());
    }
}
