package lk.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateApiKeyRequest(
        @NotBlank(message = "API key name is required")
        @Size(max = 100, message = "API key name must not exceed 100 characters")
        String name,

        Instant expiresAt
) {}
