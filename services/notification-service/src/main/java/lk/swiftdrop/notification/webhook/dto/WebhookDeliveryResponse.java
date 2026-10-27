package lk.swiftdrop.notification.webhook.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
    UUID id,
    UUID eventId,
    UUID parcelId,
    UUID merchantId,
    String webhookUrl,
    Integer httpStatus,
    int attemptNumber,
    String status,
    String failureReason,
    Instant createdAt
) {}
