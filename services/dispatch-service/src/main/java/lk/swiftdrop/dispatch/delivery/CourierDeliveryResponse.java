package lk.swiftdrop.dispatch.delivery;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.assignment.ParcelDetails;

/** One of the courier's deliveries. Parcel fields are null for assignments made before they were copied. */
public record CourierDeliveryResponse(
        UUID assignmentId,
        UUID parcelId,
        AssignmentStatus status,
        Instant assignedAt,
        String trackingNo,
        String pickupAddress,
        String dropoffAddress,
        String recipientName,
        String size,
        BigDecimal weightKg) {

    static CourierDeliveryResponse from(Assignment assignment) {
        ParcelDetails parcel = assignment.getParcelDetails();
        if (parcel == null) {
            return new CourierDeliveryResponse(assignment.getId(), assignment.getParcelId(), assignment.getStatus(),
                    assignment.getAssignedAt(), null, null, null, null, null, null);
        }
        return new CourierDeliveryResponse(assignment.getId(), assignment.getParcelId(), assignment.getStatus(),
                assignment.getAssignedAt(), parcel.trackingNo(), parcel.pickupAddress(), parcel.dropoffAddress(),
                parcel.recipientName(), parcel.size(), parcel.weightKg());
    }
}
