package lk.swiftdrop.notification.webhook.mapper;

import lk.swiftdrop.notification.webhook.dto.WebhookDeliveryResponse;
import lk.swiftdrop.notification.webhook.entity.WebhookDelivery;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryMapper {

    public WebhookDeliveryResponse toResponse(WebhookDelivery entity) {
        return new WebhookDeliveryResponse(
                entity.getId(), entity.getEventId(), entity.getParcelId(),
                entity.getMerchantId(), entity.getWebhookUrl(), entity.getHttpStatus(),
                entity.getAttemptNumber(), entity.getStatus().name(),
                entity.getFailureReason(), entity.getCreatedAt());
    }
}
