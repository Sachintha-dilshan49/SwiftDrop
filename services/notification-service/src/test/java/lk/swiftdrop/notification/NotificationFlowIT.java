package lk.swiftdrop.notification;

import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.notification.consumer.repository.ProcessedEventRepository;
import lk.swiftdrop.notification.notification.repository.NotificationRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

class NotificationFlowIT extends BaseIntegrationTest {

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @MockitoBean private JavaMailSender javaMailSender;

    @BeforeEach
    void cleanDb() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void statusChangeEventTriggersNotificationProcessing() {
        doNothing().when(javaMailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        UUID eventId = UUID.randomUUID();
        String json = buildEventJson(eventId, "CREATED", "PICKED_UP");

        rabbitTemplate.send(ParcelEvents.EXCHANGE, "parcel.status.picked_up",
                MessageBuilder.withBody(json.getBytes())
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("__TypeId__", "lk.swiftdrop.common.event.ParcelStatusChanged")
                        .build());

        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(processedEventRepository.existsByEventIdAndConsumerName(eventId, "notification"))
                        .isTrue());
    }

    @Test
    void duplicateEventIsSkipped() {
        doNothing().when(javaMailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        UUID eventId = UUID.randomUUID();
        String json = buildEventJson(eventId, "CREATED", "PICKED_UP");

        for (int i = 0; i < 2; i++) {
            rabbitTemplate.send(ParcelEvents.EXCHANGE, "parcel.status.picked_up",
                    MessageBuilder.withBody(json.getBytes())
                            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                            .setHeader("__TypeId__", "lk.swiftdrop.common.event.ParcelStatusChanged")
                            .build());
        }

        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(processedEventRepository.existsByEventIdAndConsumerName(eventId, "notification"))
                        .isTrue());

        await().pollDelay(2, SECONDS).atMost(5, SECONDS).untilAsserted(() ->
                assertThat(processedEventRepository.count()).isEqualTo(1));
    }

    private String buildEventJson(UUID eventId, String from, String to) {
        return """
                {"eventId":"%s","type":"ParcelStatusChanged","version":1,
                "occurredAt":"%s","parcelId":"%s","trackingNo":"SD-IT-TEST",
                "customerId":"%s","merchantId":null,"courierId":null,
                "fromStatus":"%s","toStatus":"%s","note":null,"failureReason":null}
                """.formatted(eventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), from, to);
    }
}

