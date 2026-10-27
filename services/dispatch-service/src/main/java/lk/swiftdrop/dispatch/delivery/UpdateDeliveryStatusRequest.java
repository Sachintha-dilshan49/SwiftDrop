package lk.swiftdrop.dispatch.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.swiftdrop.common.event.ParcelStatus;

/** Multipart form fields of a courier status update; the proof photo part is added with the upload feature. */
public record UpdateDeliveryStatusRequest(
        @NotNull ParcelStatus status,
        @Size(max = 500) String note,
        @Size(max = 150) String recipientName,
        @Size(max = 500) String failureReason) {
}
