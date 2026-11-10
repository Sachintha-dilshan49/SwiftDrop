package lk.swiftdrop.merchant.client;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateParcelInternalResponse(
        UUID id,
        String trackingNo,
        String status,
        BigDecimal feeAmount,
        String feeCurrency
) {}
