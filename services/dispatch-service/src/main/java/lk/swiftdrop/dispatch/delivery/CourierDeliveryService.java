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
import lk.swiftdrop.dispatch.proof.DeliveryProof;
import lk.swiftdrop.dispatch.proof.DeliveryProofRepository;
import lk.swiftdrop.dispatch.proof.ProofPhoto;
import lk.swiftdrop.dispatch.proof.ProofStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class CourierDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(CourierDeliveryService.class);

    private final AssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final DeliveryProofRepository deliveryProofRepository;
    private final ParcelServiceClient parcelServiceClient;
    private final ProofStorage proofStorage;
    private final TransactionOperations transactions;
    private final Clock clock;

    public CourierDeliveryService(AssignmentRepository assignmentRepository, CourierRepository courierRepository,
            DeliveryProofRepository deliveryProofRepository, ParcelServiceClient parcelServiceClient,
            ProofStorage proofStorage, TransactionOperations transactions, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.deliveryProofRepository = deliveryProofRepository;
        this.parcelServiceClient = parcelServiceClient;
        this.proofStorage = proofStorage;
        this.transactions = transactions;
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
     * Stores the optional proof photo, moves the parcel through parcel-service, then records the result.
     * Not one transaction: the remote calls must not hold a database connection, and nothing is recorded
     * until parcel-service has accepted the new status. A photo whose status change is rejected is deleted.
     *
     * @param photo the proof photo, or {@code null} when the courier sent none
     */
    public CourierDeliveryResponse updateStatus(UUID userId, UUID assignmentId,
            UpdateDeliveryStatusRequest request, ProofPhoto photo) {
        if (request.status() == ParcelStatus.FAILED
                && (request.failureReason() == null || request.failureReason().isBlank())) {
            throw new ValidationException("A failed delivery needs a reason.",
                    List.of(new FieldErrorDetail("failureReason", "is required when status is FAILED")));
        }
        ProofPhoto.ImageType photoType = photo == null ? null : photo.detectType()
                .orElseThrow(() -> new ValidationException("The proof photo must be a JPEG, PNG or WebP image.",
                        List.of(new FieldErrorDetail("photo", "must be a JPEG, PNG or WebP image"))));

        Assignment assignment = findOwnAssignment(userId, assignmentId);
        if (!AssignmentStatus.ACTIVE.contains(assignment.getStatus())) {
            throw new ConflictException("delivery-closed", "Delivery closed",
                    "Delivery " + assignmentId + " is already " + assignment.getStatus() + ".");
        }

        String proofKey = photo == null ? null : proofStorage.store(assignmentId, photo, photoType);
        try {
            parcelServiceClient.updateStatus(assignment.getParcelId(), ParcelStatusUpdate.byCourier(request.status(),
                    assignment.getCourierId(), userId, request.note(), request.recipientName(),
                    request.failureReason(), proofKey));
        } catch (RuntimeException e) {
            if (proofKey != null) {
                proofStorage.delete(proofKey);
            }
            throw e;
        }

        assignment.setStatus(switch (request.status()) {
            case DELIVERED -> AssignmentStatus.COMPLETED;
            case FAILED -> AssignmentStatus.FAILED;
            default -> AssignmentStatus.IN_PROGRESS;
        });
        assignment.setParcelStatus(request.status());
        Assignment saved = transactions.execute(tx -> {
            if (proofKey != null) {
                deliveryProofRepository.save(new DeliveryProof(assignmentId, proofKey));
            }
            return assignmentRepository.save(assignment);
        });
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
