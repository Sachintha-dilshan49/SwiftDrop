package lk.swiftdrop.notification.consumer;

import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.notification.consumer.service.IdempotencyService;
import lk.swiftdrop.notification.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StatusChangeNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatusChangeNotificationConsumer.class);
    private static final String CONSUMER_NAME = "notification";

    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;

    public StatusChangeNotificationConsumer(IdempotencyService idempotencyService,
                                           NotificationService notificationService) {
        this.idempotencyService = idempotencyService;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void onStatusChange(ParcelStatusChanged event) {
        log.info("Received ParcelStatusChanged: eventId={}, parcel={}, status={}→{}",
                event.eventId(), event.trackingNo(), event.fromStatus(), event.toStatus());

        if (idempotencyService.isAlreadyProcessed(event.eventId(), CONSUMER_NAME)) {
            log.info("Event {} already processed by {}, skipping", event.eventId(), CONSUMER_NAME);
            return;
        }
        try {
            notificationService.processStatusChange(event);
            idempotencyService.markProcessed(event.eventId(), CONSUMER_NAME);
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.eventId(), e.getMessage());
            throw e;
        }
    }
}
