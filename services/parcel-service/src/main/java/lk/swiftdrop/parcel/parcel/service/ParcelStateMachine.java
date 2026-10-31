package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * State machine enforcing valid parcel lifecycle transitions (docs/architecture.md §7).
 *
 * <pre>
 * CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
 *                                                    ↘ FAILED (reason required)
 * </pre>
 */
@Component
public class ParcelStateMachine {

    private static final Map<ParcelStatus, Set<ParcelStatus>> ALLOWED_TRANSITIONS = Map.of(
            ParcelStatus.CREATED, Set.of(ParcelStatus.PICKED_UP),
            ParcelStatus.PICKED_UP, Set.of(ParcelStatus.IN_TRANSIT),
            ParcelStatus.IN_TRANSIT, Set.of(ParcelStatus.OUT_FOR_DELIVERY),
            ParcelStatus.OUT_FOR_DELIVERY, Set.of(ParcelStatus.DELIVERED, ParcelStatus.FAILED),
            ParcelStatus.DELIVERED, Set.of(),
            ParcelStatus.FAILED, Set.of()
    );

    /**
     * Validates if the transition from current status to target status is permitted.
     */
    public void validateTransition(Parcel parcel, ParcelStatus targetStatus, String failureReason) {
        ParcelStatus currentStatus = parcel.getStatus();
        Set<ParcelStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedTargets.contains(targetStatus)) {
            throw new ConflictException(
                    "invalid-status-transition",
                    "Invalid status transition",
                    "Parcel cannot move from " + currentStatus + " to " + targetStatus + "."
            );
        }

        boolean hasReason = failureReason != null && !failureReason.isBlank();
        if (targetStatus == ParcelStatus.FAILED && !hasReason) {
            throw new ValidationException(
                    "A failure reason is required when marking a parcel as FAILED.",
                    List.of(new FieldErrorDetail("failureReason", "is required when status is FAILED"))
            );
        }
        if (targetStatus != ParcelStatus.FAILED && hasReason) {
            throw new ValidationException(
                    "A failure reason can only be given when marking a parcel as FAILED.",
                    List.of(new FieldErrorDetail("failureReason", "is only allowed when status is FAILED"))
            );
        }
    }

    /**
     * Validates whether the caller with the specified role and ID is authorized to perform this transition.
     * A caller without a role is always rejected.
     */
    public void validateAuthorization(Parcel parcel, UUID callerId, String callerRole) {
        if (callerRole == null || callerRole.isBlank()) {
            throw new ForbiddenException("A caller role is required to update parcel status.");
        }

        String normalizedRole = callerRole.toUpperCase(Locale.ROOT).replace("ROLE_", "");

        if ("ADMIN".equals(normalizedRole) || "DISPATCHER".equals(normalizedRole)) {
            return; // Dispatchers and Admins can update any parcel
        }

        if ("COURIER".equals(normalizedRole)) {
            // Without the courier's ID the assignment cannot be checked
            if (callerId == null) {
                throw new ForbiddenException("A courier must be identified to update parcel status.");
            }
            if (parcel.getCourierId() != null && !parcel.getCourierId().equals(callerId)) {
                throw new ForbiddenException("Courier is not assigned to this delivery.");
            }
            return;
        }

        throw new ForbiddenException("Role " + normalizedRole + " is not authorized to update parcel status.");
    }
}
