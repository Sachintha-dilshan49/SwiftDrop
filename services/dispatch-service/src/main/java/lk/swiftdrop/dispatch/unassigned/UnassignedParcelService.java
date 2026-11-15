package lk.swiftdrop.dispatch.unassigned;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.parcel.ParcelLookupClient;
import lk.swiftdrop.dispatch.parcel.WaitingParcel;
import org.springframework.stereotype.Service;

/**
 * Parcels waiting for a courier: CREATED in parcel-service and without an active assignment here. A parcel stays
 * CREATED until the courier picks it up, so the assignment table is what tells assigned ones apart.
 */
@Service
public class UnassignedParcelService {

    private final ParcelLookupClient parcelLookupClient;
    private final AssignmentRepository assignmentRepository;

    public UnassignedParcelService(ParcelLookupClient parcelLookupClient, AssignmentRepository assignmentRepository) {
        this.parcelLookupClient = parcelLookupClient;
        this.assignmentRepository = assignmentRepository;
    }

    /** Oldest first, from the oldest 100 CREATED parcels. */
    public List<UnassignedParcelResponse> getUnassignedParcels(String dispatcherToken) {
        List<WaitingParcel> created = parcelLookupClient.oldestCreatedParcels(dispatcherToken);
        if (created.isEmpty()) {
            return List.of();
        }
        Set<UUID> assigned = assignmentRepository.findParcelIdsWithStatus(
                created.stream().map(WaitingParcel::id).toList(), AssignmentStatus.ACTIVE);
        return created.stream()
                .filter(parcel -> !assigned.contains(parcel.id()))
                .map(UnassignedParcelResponse::from)
                .toList();
    }
}
