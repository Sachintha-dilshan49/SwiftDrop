package lk.swiftdrop.merchant.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookCreatedResponse(
        UUID id,
        String webhookUrl,
        String secret,
        boolean active,
        Instant createdAt
) {}
