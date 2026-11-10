package lk.swiftdrop.dispatch.delivery;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;

public record CourierDeliveryResponse(
        UUID assignmentId,
        UUID parcelId,
        AssignmentStatus status,
        Instant assignedAt) {

    static CourierDeliveryResponse from(Assignment assignment) {
        return new CourierDeliveryResponse(
                assignment.getId(), assignment.getParcelId(), assignment.getStatus(), assignment.getAssignedAt());
    }
}
