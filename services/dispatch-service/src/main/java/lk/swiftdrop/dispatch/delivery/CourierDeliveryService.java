package lk.swiftdrop.dispatch.delivery;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.parcel.ParcelServiceClient;
import lk.swiftdrop.dispatch.parcel.ParcelStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourierDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(CourierDeliveryService.class);

    private final AssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final ParcelServiceClient parcelServiceClient;
    private final Clock clock;

    public CourierDeliveryService(AssignmentRepository assignmentRepository, CourierRepository courierRepository,
            ParcelServiceClient parcelServiceClient, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.parcelServiceClient = parcelServiceClient;
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

    /**
     * Moves the parcel through parcel-service, then records the result on the assignment. Not one
     * transaction: the remote call must not hold a database connection, and the assignment is only
     * changed after parcel-service has accepted the new status.
     */
    public CourierDeliveryResponse updateStatus(UUID userId, String accessToken, UUID assignmentId,
            UpdateDeliveryStatusRequest request) {
        if (request.status() == ParcelStatus.FAILED
                && (request.failureReason() == null || request.failureReason().isBlank())) {
            throw new ValidationException("A failed delivery needs a reason.",
                    List.of(new FieldErrorDetail("failureReason", "is required when status is FAILED")));
        }

        Assignment assignment = findOwnAssignment(userId, assignmentId);
        if (!AssignmentStatus.ACTIVE.contains(assignment.getStatus())) {
            throw new ConflictException("delivery-closed", "Delivery closed",
                    "Delivery " + assignmentId + " is already " + assignment.getStatus() + ".");
        }

        parcelServiceClient.updateStatus(assignment.getParcelId(), new ParcelStatusUpdate(request.status(),
                request.note(), request.recipientName(), request.failureReason(), assignment.getCourierId(), null),
                accessToken);

        assignment.setStatus(switch (request.status()) {
            case DELIVERED -> AssignmentStatus.COMPLETED;
            case FAILED -> AssignmentStatus.FAILED;
            default -> AssignmentStatus.IN_PROGRESS;
        });
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Delivery {} moved parcel {} to {}", assignmentId, assignment.getParcelId(), request.status());
        return CourierDeliveryResponse.from(saved);
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
