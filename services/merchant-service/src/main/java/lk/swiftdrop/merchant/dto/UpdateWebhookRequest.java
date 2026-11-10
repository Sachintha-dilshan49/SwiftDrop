package lk.swiftdrop.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWebhookRequest(
        @NotBlank(message = "Webhook URL is required")
        @Pattern(regexp = "^https?://.+", message = "Webhook URL must be a valid HTTP or HTTPS URL")
        @Size(max = 1000, message = "Webhook URL must not exceed 1000 characters")
        String webhookUrl,

        boolean rotateSecret
) {}
