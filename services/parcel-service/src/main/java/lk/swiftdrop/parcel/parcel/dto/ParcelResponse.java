package lk.swiftdrop.parcel.parcel.dto;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ParcelResponse(
        UUID id,
        String trackingNo,
        UUID customerId,
        UUID merchantId,
        UUID courierId,
        String pickupAddress,
        String dropoffAddress,
        String recipientName,
        String recipientPhone,
        ParcelSize size,
        BigDecimal weightKg,
        BigDecimal feeAmount,
        String feeCurrency,
        ParcelStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        List<StatusHistoryResponse> history
) {

    public static ParcelResponse fromEntity(Parcel parcel) {
        List<StatusHistoryResponse> historyList = parcel.getStatusHistory() != null
                ? parcel.getStatusHistory().stream()
                .map(h -> new StatusHistoryResponse(
                        h.getStatus(),
                        h.getChangedBy(),
                        h.getChangedByRole(),
                        h.getNote(),
                        h.getProofReference(),
                        h.getCreatedAt()
                ))
                .toList()
                : List.of();

        return new ParcelResponse(
                parcel.getId(),
                parcel.getTrackingNo(),
                parcel.getCustomerId(),
                parcel.getMerchantId(),
                parcel.getCourierId(),
                parcel.getPickupAddress(),
                parcel.getDropoffAddress(),
                parcel.getRecipientName(),
                parcel.getRecipientPhone(),
                parcel.getSize(),
                parcel.getWeightKg(),
                parcel.getFeeAmount(),
                parcel.getFeeCurrency(),
                parcel.getStatus(),
                parcel.getFailureReason(),
                parcel.getCreatedAt(),
                parcel.getUpdatedAt(),
                historyList
        );
    }
}
