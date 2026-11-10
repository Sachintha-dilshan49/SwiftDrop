package lk.swiftdrop.dispatch.assignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID parcelId,
        UUID courierId,
        UUID assignedByUserId,
        AssignmentStatus status,
        Instant assignedAt) {

    static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getParcelId(),
                assignment.getCourierId(),
                assignment.getAssignedByUserId(),
                assignment.getStatus(),
                assignment.getAssignedAt());
    }
}
