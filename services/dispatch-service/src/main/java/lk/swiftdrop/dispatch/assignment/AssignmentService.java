package lk.swiftdrop.dispatch.assignment;

import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, CourierRepository courierRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
    }

    @Transactional
    public AssignmentResponse assign(UUID parcelId, UUID courierId, UUID assignedByUserId) {
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

        Assignment assignment = new Assignment();
        assignment.setParcelId(parcelId);
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(assignedByUserId);
        assignment.setStatus(AssignmentStatus.ASSIGNED);

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
