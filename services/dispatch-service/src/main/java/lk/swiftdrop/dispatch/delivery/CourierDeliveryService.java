package lk.swiftdrop.dispatch.delivery;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourierDeliveryService {

    private final AssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final Clock clock;

    public CourierDeliveryService(AssignmentRepository assignmentRepository, CourierRepository courierRepository,
            Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CourierDeliveryResponse> getTodaysDeliveries(UUID userId) {
        Courier courier = currentCourier(userId);
        Instant startOfToday = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        return assignmentRepository.findDayList(courier.getId(), AssignmentStatus.ACTIVE, startOfToday).stream()
                .map(CourierDeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourierDeliveryResponse getDelivery(UUID userId, UUID assignmentId) {
        return CourierDeliveryResponse.from(findOwnAssignment(userId, assignmentId));
    }

    /** Loads an assignment and checks it belongs to the courier behind {@code userId}. */
    Assignment findOwnAssignment(UUID userId, UUID assignmentId) {
        Courier courier = currentCourier(userId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("delivery-not-found", "Delivery not found",
                        "Delivery " + assignmentId + " does not exist."));
        if (!assignment.getCourierId().equals(courier.getId())) {
            throw new ForbiddenException("not-your-delivery", "Not your delivery",
                    "Delivery " + assignmentId + " is assigned to another courier.");
        }
        return assignment;
    }

    private Courier currentCourier(UUID userId) {
        return courierRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("courier-profile-not-found", "Courier profile not found",
                        "No courier profile exists for this account."));
    }
}
