package lk.swiftdrop.merchant.dto;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.MerchantApiKey;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        boolean revoked,
        Instant revokedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static ApiKeyResponse from(MerchantApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.isRevoked(),
                apiKey.getRevokedAt(),
                apiKey.getExpiresAt(),
                apiKey.getCreatedAt()
        );
    }
}
