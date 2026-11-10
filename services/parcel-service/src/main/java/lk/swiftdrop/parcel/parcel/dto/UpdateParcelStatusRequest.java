package lk.swiftdrop.parcel.parcel.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.swiftdrop.common.event.ParcelStatus;

import java.util.UUID;

public record UpdateParcelStatusRequest(
        @NotNull(message = "Status is required")
        ParcelStatus status,

        UUID courierId,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note,

        @Size(max = 255, message = "Proof reference cannot exceed 255 characters")
        String proofReference,

        @Size(max = 500, message = "Failure reason cannot exceed 500 characters")
        String failureReason,

        UUID changedBy,
        String changedByRole
) {
}
