package lk.swiftdrop.notification.webhook.service;

import lk.swiftdrop.notification.webhook.entity.WebhookDelivery;
import lk.swiftdrop.notification.webhook.entity.WebhookDeliveryStatus;
import lk.swiftdrop.notification.webhook.repository.WebhookDeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository repository;

    public WebhookDeliveryService(WebhookDeliveryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WebhookDelivery recordSuccess(UUID eventId, UUID parcelId, UUID merchantId,
                                         String webhookUrl, int attemptNumber,
                                         String requestBody, int httpStatus, String responseBody) {
        var delivery = new WebhookDelivery(eventId, parcelId, merchantId, webhookUrl, attemptNumber, requestBody);
        delivery.markSuccess(httpStatus, responseBody);
        return repository.save(delivery);
    }

    @Transactional
    public WebhookDelivery recordFailure(UUID eventId, UUID parcelId, UUID merchantId,
                                         String webhookUrl, int attemptNumber,
                                         String requestBody, int httpStatus,
                                         String responseBody, String reason) {
        var delivery = new WebhookDelivery(eventId, parcelId, merchantId, webhookUrl, attemptNumber, requestBody);
        delivery.markFailed(httpStatus, responseBody, reason);
        return repository.save(delivery);
    }

    @Transactional
    public WebhookDelivery recordParked(UUID eventId, UUID parcelId, UUID merchantId,
                                        String webhookUrl, int attemptNumber,
                                        String requestBody, String reason) {
        var delivery = new WebhookDelivery(eventId, parcelId, merchantId, webhookUrl, attemptNumber, requestBody);
        delivery.markParked(reason);
        return repository.save(delivery);
    }

    public Page<WebhookDelivery> listAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<WebhookDelivery> listByStatus(WebhookDeliveryStatus status, Pageable pageable) {
        return repository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public Optional<WebhookDelivery> findById(UUID id) {
        return repository.findById(id);
    }
}
