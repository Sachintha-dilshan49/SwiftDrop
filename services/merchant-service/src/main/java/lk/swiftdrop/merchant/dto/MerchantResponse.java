package lk.swiftdrop.merchant.dto;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;

public record MerchantResponse(
        UUID id,
        UUID userId,
        String businessName,
        String businessRegNo,
        String contactName,
        String email,
        String phone,
        String address,
        MerchantStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getUserId(),
                merchant.getBusinessName(),
                merchant.getBusinessRegNo(),
                merchant.getContactName(),
                merchant.getEmail(),
                merchant.getPhone(),
                merchant.getAddress(),
                merchant.getStatus(),
                merchant.getRejectionReason(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()
        );
    }
}
