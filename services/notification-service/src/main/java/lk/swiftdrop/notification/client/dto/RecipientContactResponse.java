package lk.swiftdrop.notification.client.dto;

/**
 * Contact details of the physical recipient of a parcel.
 * Returned by parcel-service GET /internal/parcels/{id}/contacts (ADR-010).
 *
 * recipientEmail is null for customer bookings (the parcel was booked by a customer directly,
 * so their email is available from auth-service instead).
 */
public record RecipientContactResponse(
        String recipientName,
        String recipientPhone,
        String recipientEmail
) {}

