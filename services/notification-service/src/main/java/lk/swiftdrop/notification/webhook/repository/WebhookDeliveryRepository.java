package lk.swiftdrop.notification.webhook.repository;

import lk.swiftdrop.notification.webhook.entity.WebhookDelivery;
import lk.swiftdrop.notification.webhook.entity.WebhookDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    Page<WebhookDelivery> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<WebhookDelivery> findByStatusOrderByCreatedAtDesc(WebhookDeliveryStatus status, Pageable pageable);
}
