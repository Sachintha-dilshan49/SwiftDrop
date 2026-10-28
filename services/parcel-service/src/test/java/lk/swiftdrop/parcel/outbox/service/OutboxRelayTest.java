package lk.swiftdrop.parcel.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.config.JacksonConfig;
import lk.swiftdrop.parcel.outbox.config.OutboxProperties;
import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import lk.swiftdrop.parcel.outbox.entity.OutboxStatus;
import lk.swiftdrop.parcel.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = JacksonConfig.eventObjectMapper();
    private final OutboxProperties properties =
            new OutboxProperties(50, Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofSeconds(60), 20);

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxEventRepository, rabbitTemplate, objectMapper, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Does nothing when no event is due")
    void publishesNothingWhenNoEventIsDue() {
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of());

        outboxRelay.publishPendingEvents();

        verifyNoInteractions(rabbitTemplate);
        verify(outboxEventRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("An event confirmed by the broker is marked SENT and published with messageId = eventId")
    void marksConfirmedEventSent() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        whenPublished(confirm(true, null));

        outboxRelay.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt()).isEqualTo(NOW);
        assertThat(event.getLastError()).isNull();
        assertThat(event.getRetryCount()).isZero();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(ParcelEvents.EXCHANGE), eq(event.getRoutingKey()), payload.capture(),
                postProcessor.capture(), any(CorrelationData.class));
        assertThat(payload.getValue()).isInstanceOf(ParcelStatusChanged.class);
        assertThat(((ParcelStatusChanged) payload.getValue()).eventId()).isEqualTo(event.getEventId());

        Message message = postProcessor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.getEventId().toString());
        verify(outboxEventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("A nacked event stays PENDING and is retried after the backoff")
    void retriesNackedEvent() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        whenPublished(confirm(false, "queue limit reached"));

        outboxRelay.publishPendingEvents();

        assertRetried(event, 1, Duration.ofSeconds(1));
        assertThat(event.getLastError()).contains("nacked").contains("queue limit reached");
    }

    @Test
    @DisplayName("An event returned as unroutable stays PENDING even though the broker acked it")
    void retriesReturnedEvent() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        whenPublished(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                    312, "NO_ROUTE", ParcelEvents.EXCHANGE, event.getRoutingKey()));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        });

        outboxRelay.publishPendingEvents();

        assertRetried(event, 1, Duration.ofSeconds(1));
        assertThat(event.getLastError()).contains("unroutable").contains("NO_ROUTE");
    }

    @Test
    @DisplayName("An event without a publisher confirm in time stays PENDING")
    void retriesEventWithoutConfirm() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        whenPublished(invocation -> null);

        outboxRelay.publishPendingEvents();

        assertRetried(event, 1, Duration.ofSeconds(1));
        assertThat(event.getLastError()).contains("No publisher confirm");
    }

    @Test
    @DisplayName("An event stays PENDING when the broker is unreachable")
    void retriesWhenBrokerIsDown() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        outboxRelay.publishPendingEvents();

        assertRetried(event, 1, Duration.ofSeconds(1));
        assertThat(event.getLastError()).contains("Publish failed");
    }

    @Test
    @DisplayName("Transient failures are never given up on, however many attempts have failed")
    void neverGivesUpOnTransientFailures() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        event.setRetryCount(500);
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        whenPublished(confirm(false, "still down"));

        outboxRelay.publishPendingEvents();

        assertRetried(event, 501, Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("Retry delay doubles from 1 s and is capped at 60 s")
    void backoffDoublesAndIsCapped() {
        assertThat(outboxRelay.backoff(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(outboxRelay.backoff(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(outboxRelay.backoff(3)).isEqualTo(Duration.ofSeconds(4));
        assertThat(outboxRelay.backoff(6)).isEqualTo(Duration.ofSeconds(32));
        assertThat(outboxRelay.backoff(7)).isEqualTo(Duration.ofSeconds(60));
        assertThat(outboxRelay.backoff(10_000)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("An unreadable payload is PARKED and not published")
    void parksUnreadablePayload() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ParcelStatusChanged.TYPE, ParcelEvents.routingKey(ParcelStatus.CREATED), "{not json");
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));

        outboxRelay.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PARKED);
        assertThat(event.getLastError()).startsWith("Unreadable payload");
        verifyNoInteractions(rabbitTemplate);
        verify(outboxEventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("A failing event of one parcel does not stop events of other parcels")
    void failureOfOneEventDoesNotBlockOthers() {
        OutboxEvent failing = pendingEvent(UUID.randomUUID());
        OutboxEvent healthy = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(failing, healthy));
        whenPublished(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            boolean ack = !correlation.getId().equals(failing.getEventId().toString());
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack"));
            return null;
        });

        outboxRelay.publishPendingEvents();

        assertThat(failing.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(healthy.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    @DisplayName("Stored error text is limited to the column size")
    void truncatesLongErrors() {
        OutboxEvent event = pendingEvent(UUID.randomUUID());
        when(outboxEventRepository.lockNextDueEvents(NOW, 50)).thenReturn(List.of(event));
        whenPublished(confirm(false, "x".repeat(2_000)));

        outboxRelay.publishPendingEvents();

        assertThat(event.getLastError()).hasSize(500);
    }

    @Test
    @DisplayName("Asks the repository only for events due now, in batches of the configured size")
    void locksDueEventsInBatches() {
        when(outboxEventRepository.lockNextDueEvents(any(Instant.class), anyInt())).thenReturn(List.of());

        outboxRelay.publishPendingEvents();

        verify(outboxEventRepository).lockNextDueEvents(NOW, 50);
    }

    private OutboxEvent pendingEvent(UUID parcelId) {
        try {
            ParcelStatusChanged event = ParcelStatusChanged.of(NOW.minusSeconds(5), parcelId, "SD-7K3Q-9P2M",
                    UUID.randomUUID(), null, null, null, ParcelStatus.CREATED, "Booking created", null);
            return new OutboxEvent(UUID.randomUUID(), event.eventId(), parcelId, ParcelStatusChanged.TYPE,
                    ParcelEvents.routingKey(ParcelStatus.CREATED), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void whenPublished(Answer<Void> answer) {
        doAnswer(answer).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    private static Answer<Void> confirm(boolean ack, String reason) {
        return invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        };
    }

    private static void assertRetried(OutboxEvent event, int attempts, Duration delay) {
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(attempts);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plus(delay));
        assertThat(event.getSentAt()).isNull();
    }
}
