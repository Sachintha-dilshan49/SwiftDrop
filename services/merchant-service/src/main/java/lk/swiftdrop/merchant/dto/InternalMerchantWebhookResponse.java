package lk.swiftdrop.merchant.dto;

public record InternalMerchantWebhookResponse(
        String webhookUrl,
        String secret
) {}
