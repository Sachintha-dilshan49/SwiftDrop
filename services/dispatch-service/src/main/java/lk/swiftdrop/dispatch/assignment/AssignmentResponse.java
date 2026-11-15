package lk.swiftdrop.dispatch.assignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID parcelId,
        String trackingNo,
        UUID courierId,
        UUID assignedByUserId,
        AssignmentStatus status,
        Instant assignedAt) {

    static AssignmentResponse from(Assignment assignment) {
        ParcelDetails parcel = assignment.getParcelDetails();
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getParcelId(),
                parcel == null ? null : parcel.trackingNo(),
                assignment.getCourierId(),
                assignment.getAssignedByUserId(),
                assignment.getStatus(),
                assignment.getAssignedAt());
    }
}
