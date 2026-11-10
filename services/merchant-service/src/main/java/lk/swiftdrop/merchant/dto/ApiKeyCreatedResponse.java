package lk.swiftdrop.merchant.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyCreatedResponse(
        UUID id,
        String name,
        String keyPrefix,
        String rawApiKey,
        Instant expiresAt,
        Instant createdAt
) {}
