package lk.swiftdrop.dispatch.assignment;

import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import lk.swiftdrop.dispatch.parcel.AssignableParcel;
import lk.swiftdrop.dispatch.parcel.ParcelLookupClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final ParcelLookupClient parcelLookupClient;

    public AssignmentService(AssignmentRepository assignmentRepository, CourierRepository courierRepository,
            ParcelLookupClient parcelLookupClient) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.parcelLookupClient = parcelLookupClient;
    }

    /**
     * Not one transaction on purpose: parcel-service is called in the middle, and a DB connection should not wait on
     * it. {@code ux_assignments_active_parcel} still stops two concurrent assignments of the same parcel.
     */
    public AssignmentResponse assign(UUID parcelId, UUID courierId, UUID assignedByUserId, String dispatcherToken) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new NotFoundException("courier-not-found", "Courier not found",
                        "Courier " + courierId + " does not exist."));

        if (courier.getStatus() == CourierStatus.OFFLINE) {
            throw new ConflictException("courier-unavailable", "Courier unavailable",
                    "Courier " + courierId + " is offline and cannot take new deliveries.");
        }
        if (assignmentRepository.existsByParcelIdAndStatusIn(parcelId, AssignmentStatus.ACTIVE)) {
            throw alreadyAssigned(parcelId);
        }

        AssignableParcel parcel = parcelLookupClient.getParcel(parcelId, dispatcherToken);
        if (parcel.status() != ParcelStatus.CREATED) {
            throw new ConflictException("parcel-not-waiting", "Parcel not waiting for a courier",
                    "Parcel " + parcelId + " is " + parcel.status() + "; only CREATED parcels can be assigned.");
        }

        Assignment assignment = new Assignment();
        assignment.setParcelId(parcelId);
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(assignedByUserId);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setParcelDetails(new ParcelDetails(parcel.trackingNo(), parcel.pickupAddress(),
                parcel.dropoffAddress(), parcel.recipientName(), parcel.size(), parcel.weightKg()));

        try {
            // flush now so a concurrent assignment hits ux_assignments_active_parcel here, not at commit
            assignment = assignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException e) {
            throw alreadyAssigned(parcelId);
        }

        log.info("Parcel {} assigned to courier {}", parcelId, courierId);
        return AssignmentResponse.from(assignment);
    }

    private static ConflictException alreadyAssigned(UUID parcelId) {
        return new ConflictException("parcel-already-assigned", "Parcel already assigned",
                "Parcel " + parcelId + " already has an active assignment.");
    }
}
