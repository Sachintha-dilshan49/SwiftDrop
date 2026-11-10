package lk.swiftdrop.merchant.client;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateParcelInternalRequest(
        UUID merchantId,
        String pickupAddress,
        String dropoffAddress,
        String recipientName,
        String recipientPhone,
        String recipientEmail,
        String pickupZone,
        String dropoffZone,
        String size,
        String speed,
        BigDecimal weightKg
) {}
