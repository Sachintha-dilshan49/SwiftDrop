package lk.swiftdrop.merchant.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.MerchantOrder;

public record MerchantOrderResponse(
        UUID id,
        UUID parcelId,
        String trackingNo,
        String recipientName,
        String recipientPhone,
        String dropoffAddress,
        String status,
        BigDecimal feeAmount,
        String feeCurrency,
        Instant createdAt
) {
    public static MerchantOrderResponse from(MerchantOrder order) {
        return new MerchantOrderResponse(
                order.getId(),
                order.getParcelId(),
                order.getTrackingNo(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getDropoffAddress(),
                order.getStatus(),
                order.getFeeAmount(),
                order.getFeeCurrency(),
                order.getCreatedAt()
        );
    }
}
