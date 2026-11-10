package lk.swiftdrop.notification.consumer;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.notification.client.MerchantServiceClient;
import lk.swiftdrop.notification.client.dto.MerchantWebhookResponse;
import lk.swiftdrop.notification.consumer.service.IdempotencyService;
import lk.swiftdrop.notification.webhook.service.WebhookDeliveryService;
import lk.swiftdrop.notification.webhook.service.WebhookSender;
import lk.swiftdrop.notification.webhook.service.WebhookSender.WebhookSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookConsumerTest {

    @Mock private IdempotencyService idempotencyService;
    @Mock private MerchantServiceClient merchantServiceClient;
    @Mock private WebhookSender webhookSender;
    @Mock private WebhookDeliveryService webhookDeliveryService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private Jackson2JsonMessageConverter messageConverter;

    private WebhookConsumer consumer;

    private static final UUID MERCHANT_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final MerchantWebhookResponse WEBHOOK_CONFIG =
            new MerchantWebhookResponse("https://store.example.com/hook", "secret123");

    @BeforeEach
    void setUp() {
        consumer = new WebhookConsumer(idempotencyService, merchantServiceClient, webhookSender,
                webhookDeliveryService, rabbitTemplate, messageConverter);
        ReflectionTestUtils.setField(consumer, "retryQueue10s", "webhook.retry.10s");
        ReflectionTestUtils.setField(consumer, "retryQueue1m", "webhook.retry.1m");
        ReflectionTestUtils.setField(consumer, "retryQueue5m", "webhook.retry.5m");
        ReflectionTestUtils.setField(consumer, "parkingQueue", "webhook.parking");
    }

    @Test
    void skipsEventWithoutMerchantId() {
        ParcelStatusChanged event = buildEvent(null);
        Message message = buildMessage(0);
        when(messageConverter.fromMessage(message)).thenReturn(event);
        consumer.onStatusChange(message);
        verify(webhookSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void skipsDuplicateEventOnFirstAttempt() {
        ParcelStatusChanged event = buildEvent(MERCHANT_ID);
        Message message = buildMessage(0);
        when(messageConverter.fromMessage(message)).thenReturn(event);
        when(idempotencyService.isAlreadyProcessed(event.eventId(), "webhook")).thenReturn(true);
        consumer.onStatusChange(message);
        verify(webhookSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deliversWebhookSuccessfully() {
        ParcelStatusChanged event = buildEvent(MERCHANT_ID);
        Message message = buildMessage(0);
        when(messageConverter.fromMessage(message)).thenReturn(event);
        when(idempotencyService.isAlreadyProcessed(any(), any())).thenReturn(false);
        when(merchantServiceClient.getWebhookConfig(MERCHANT_ID)).thenReturn(Optional.of(WEBHOOK_CONFIG));
        when(webhookSender.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new WebhookSendResult(true, 200, "OK", null));
        consumer.onStatusChange(message);
        verify(webhookDeliveryService).recordSuccess(eq(event.eventId()), any(), eq(MERCHANT_ID),
                eq("https://store.example.com/hook"), eq(1), anyString(), eq(200), eq("OK"));
        verify(idempotencyService).markProcessed(event.eventId(), "webhook");
    }

    @Test
    void routesToRetry10sOnFirstFailure() {
        ParcelStatusChanged event = buildEvent(MERCHANT_ID);
        Message message = buildMessage(0);
        when(messageConverter.fromMessage(message)).thenReturn(event);
        when(idempotencyService.isAlreadyProcessed(any(), any())).thenReturn(false);
        when(merchantServiceClient.getWebhookConfig(MERCHANT_ID)).thenReturn(Optional.of(WEBHOOK_CONFIG));
        when(webhookSender.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new WebhookSendResult(false, 500, null, "Server error"));
        consumer.onStatusChange(message);
        verify(rabbitTemplate).send(eq(""), eq("webhook.retry.10s"), any(Message.class));
    }

    @Test
    void routesToParkingAfterThirdRetry() {
        ParcelStatusChanged event = buildEvent(MERCHANT_ID);
        Message message = buildMessage(3);
        when(messageConverter.fromMessage(message)).thenReturn(event);
        when(merchantServiceClient.getWebhookConfig(MERCHANT_ID)).thenReturn(Optional.of(WEBHOOK_CONFIG));
        when(webhookSender.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new WebhookSendResult(false, 500, null, "Server error"));
        consumer.onStatusChange(message);
        verify(rabbitTemplate).send(eq(""), eq("webhook.parking"), any(Message.class));
    }

    private ParcelStatusChanged buildEvent(UUID merchantId) {
        return new ParcelStatusChanged(EVENT_ID, ParcelStatusChanged.TYPE, ParcelStatusChanged.VERSION,
                Instant.now(), UUID.randomUUID(), "SD-TEST-0001", UUID.randomUUID(),
                merchantId, null, ParcelStatus.CREATED, ParcelStatus.PICKED_UP, null, null);
    }

    private Message buildMessage(int retryCount) {
        String json = "{\"eventId\":\"%s\"}".formatted(EVENT_ID);
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        if (retryCount > 0) { props.setHeader("x-retry-count", retryCount); }
        return new Message(json.getBytes(), props);
    }
}
