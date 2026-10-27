package lk.swiftdrop.notification.consumer;

import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.notification.client.MerchantServiceClient;
import lk.swiftdrop.notification.client.dto.MerchantWebhookResponse;
import lk.swiftdrop.notification.consumer.service.IdempotencyService;
import lk.swiftdrop.notification.webhook.service.WebhookDeliveryService;
import lk.swiftdrop.notification.webhook.service.WebhookSender;
import lk.swiftdrop.notification.webhook.service.WebhookSender.WebhookSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class WebhookConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookConsumer.class);
    private static final String CONSUMER_NAME = "webhook";
    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    private final IdempotencyService idempotencyService;
    private final MerchantServiceClient merchantServiceClient;
    private final WebhookSender webhookSender;
    private final WebhookDeliveryService webhookDeliveryService;
    private final RabbitTemplate rabbitTemplate;
    private final Jackson2JsonMessageConverter messageConverter;

    @Value("${rabbitmq.queues.webhook-retry-10s}")
    private String retryQueue10s;

    @Value("${rabbitmq.queues.webhook-retry-1m}")
    private String retryQueue1m;

    @Value("${rabbitmq.queues.webhook-retry-5m}")
    private String retryQueue5m;

    @Value("${rabbitmq.queues.webhook-parking}")
    private String parkingQueue;

    public WebhookConsumer(IdempotencyService idempotencyService,
                           MerchantServiceClient merchantServiceClient,
                           WebhookSender webhookSender,
                           WebhookDeliveryService webhookDeliveryService,
                           RabbitTemplate rabbitTemplate,
                           Jackson2JsonMessageConverter messageConverter) {
        this.idempotencyService = idempotencyService;
        this.merchantServiceClient = merchantServiceClient;
        this.webhookSender = webhookSender;
        this.webhookDeliveryService = webhookDeliveryService;
        this.rabbitTemplate = rabbitTemplate;
        this.messageConverter = messageConverter;
    }

    @RabbitListener(queues = "${rabbitmq.queues.webhook}")
    public void onStatusChange(Message message) {
        ParcelStatusChanged event = (ParcelStatusChanged) messageConverter.fromMessage(message);
        Integer retryHeader = message.getMessageProperties().getHeader(RETRY_COUNT_HEADER);
        int retryCount = retryHeader != null ? retryHeader : 0;
        int attemptNumber = retryCount + 1;

        log.info("Webhook consumer: eventId={}, attempt={}, status={}",
                event.eventId(), attemptNumber, event.toStatus());

        if (event.merchantId() == null) {
            log.debug("No merchantId in event {}, skipping webhook", event.eventId());
            return;
        }

        if (retryCount == 0 && idempotencyService.isAlreadyProcessed(event.eventId(), CONSUMER_NAME)) {
            log.info("Event {} already processed by webhook consumer, skipping", event.eventId());
            return;
        }

        Optional<MerchantWebhookResponse> config = merchantServiceClient.getWebhookConfig(event.merchantId());
        if (config.isEmpty()) {
            log.warn("No webhook config for merchant {}, parking event {}", event.merchantId(), event.eventId());
            webhookDeliveryService.recordParked(event.eventId(), event.parcelId(), event.merchantId(),
                    "unknown", attemptNumber, messageBodyAsString(message), "Merchant webhook config not found");
            return;
        }

        MerchantWebhookResponse webhook = config.get();
        String requestBody = messageBodyAsString(message);

        WebhookSendResult result = webhookSender.send(
                webhook.webhookUrl(), requestBody, webhook.secret(), event.eventId().toString());

        if (result.success()) {
            webhookDeliveryService.recordSuccess(event.eventId(), event.parcelId(), event.merchantId(),
                    webhook.webhookUrl(), attemptNumber, requestBody, result.httpStatus(), result.responseBody());
            idempotencyService.markProcessed(event.eventId(), CONSUMER_NAME);
            log.info("Webhook delivered for event {}", event.eventId());
        } else {
            webhookDeliveryService.recordFailure(event.eventId(), event.parcelId(), event.merchantId(),
                    webhook.webhookUrl(), attemptNumber, requestBody,
                    result.httpStatus(), result.responseBody(), result.failureReason());
            routeToRetry(message, retryCount, event);
        }
    }

    private void routeToRetry(Message originalMessage, int currentRetryCount, ParcelStatusChanged event) {
        int nextRetryCount = currentRetryCount + 1;

        String targetQueue = switch (currentRetryCount) {
            case 0 -> retryQueue10s;
            case 1 -> retryQueue1m;
            case 2 -> retryQueue5m;
            default -> parkingQueue;
        };

        Message retryMessage = MessageBuilder
                .withBody(originalMessage.getBody())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader(RETRY_COUNT_HEADER, nextRetryCount)
                .build();

        rabbitTemplate.send("", targetQueue, retryMessage);

        if (targetQueue.equals(parkingQueue)) {
            log.warn("Event {} parked after {} attempts", event.eventId(), nextRetryCount);
        } else {
            log.info("Event {} routed to {} (attempt {} failed)", event.eventId(), targetQueue, nextRetryCount);
        }
    }

    private String messageBodyAsString(Message message) {
        return new String(message.getBody());
    }
}
