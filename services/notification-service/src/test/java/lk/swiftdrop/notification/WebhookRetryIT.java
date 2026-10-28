package lk.swiftdrop.notification;

import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.notification.consumer.repository.ProcessedEventRepository;
import lk.swiftdrop.notification.webhook.entity.WebhookDeliveryStatus;
import lk.swiftdrop.notification.webhook.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Instant;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WebhookRetryIT extends BaseIntegrationTest {

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private WebhookDeliveryRepository webhookDeliveryRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @MockitoBean private JavaMailSender javaMailSender;

    @BeforeEach
    void cleanDb() {
        webhookDeliveryRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void eventWithMerchantIdButNoWebhookConfigGetsParked() {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        String json = buildEventJsonWithMerchant(eventId, merchantId);

        rabbitTemplate.send(ParcelEvents.EXCHANGE, "parcel.status.picked_up",
                MessageBuilder.withBody(json.getBytes())
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("__TypeId__", "lk.swiftdrop.common.event.ParcelStatusChanged")
                        .build());

        await().atMost(15, SECONDS).untilAsserted(() -> {
            var deliveries = webhookDeliveryRepository.findAll();
            assertThat(deliveries).isNotEmpty();
            assertThat(deliveries.get(0).getStatus()).isEqualTo(WebhookDeliveryStatus.PARKED);
            assertThat(deliveries.get(0).getFailureReason()).contains("not found");
        });
    }

    private String buildEventJsonWithMerchant(UUID eventId, UUID merchantId) {
        return """
                {"eventId":"%s","type":"ParcelStatusChanged","version":1,
                "occurredAt":"%s","parcelId":"%s","trackingNo":"SD-WH-TEST",
                "customerId":"%s","merchantId":"%s","courierId":null,
                "fromStatus":"CREATED","toStatus":"PICKED_UP","note":null,"failureReason":null}
                """.formatted(eventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), merchantId);
    }
}

