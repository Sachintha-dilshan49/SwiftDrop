package lk.swiftdrop.merchant.dto;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.MerchantWebhookEndpoint;

public record WebhookSettingsResponse(
        UUID id,
        String webhookUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookSettingsResponse from(MerchantWebhookEndpoint endpoint) {
        return new WebhookSettingsResponse(
                endpoint.getId(),
                endpoint.getWebhookUrl(),
                endpoint.isActive(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt()
        );
    }
}
