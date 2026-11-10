package lk.swiftdrop.parcel.tracking;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import lk.swiftdrop.parcel.outbox.repository.OutboxEventRepository;
import lk.swiftdrop.parcel.outbox.service.OutboxRelay;
import lk.swiftdrop.parcel.parcel.client.PricingQuote;
import lk.swiftdrop.parcel.parcel.client.PricingServiceClient;
import lk.swiftdrop.parcel.parcel.dto.CreateParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.UpdateParcelStatusRequest;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.entity.RouteType;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import lk.swiftdrop.parcel.support.AbstractContainersIT;
import lk.swiftdrop.parcel.tracking.config.TrackingSseProperties;
import lk.swiftdrop.parcel.tracking.repository.ProcessedEventRepository;
import lk.swiftdrop.parcel.tracking.service.TrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Live tracking end to end against real PostgreSQL and RabbitMQ: a status change is stored with its outbox
 * event, the relay publishes it to {@code parcel.events}, and the tracking consumer receives it, records it
 * in {@code processed_events} and pushes it to the open streams.
 *
 * <ul>
 *   <li>This class has its own Spring context, so it listens on its own queue: otherwise the
 *       {@code tracking.q} listener of another cached test context could take its messages.</li>
 *   <li>The listener thread calls the tracking service while tests run (the relay also publishes events
 *       other tests left behind), so a thread-safe recording subclass is used instead of a Mockito spy,
 *       which must not be stubbed while another thread uses it.</li>
 * </ul>
 */
@TestPropertySource(properties = "rabbitmq.queues.tracking=" + TrackingEventFlowIT.QUEUE)
class TrackingEventFlowIT extends AbstractContainersIT {

    static final String QUEUE = "tracking.q.flow-it";

    /** 650 + 80 + 100 + 0 - 180 = 650. No pricing-service runs in this test. */
    private static final PricingQuote QUOTE = new PricingQuote(RouteType.INTER_CITY, new BigDecimal("650.00"),
            new BigDecimal("80.00"), new BigDecimal("100.00"), new BigDecimal("0.00"), new BigDecimal("180.00"),
            new BigDecimal("650.00"), "LKR");

    @MockitoBean
    private PricingServiceClient pricingServiceClient;

    @Autowired
    private RecordingTrackingService trackingService;

    @Autowired
    private ParcelService parcelService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("A booking and a courier's status change reach the live tracking streams through the outbox and RabbitMQ")
    void statusChangesReachLiveTracking() {
        given(pricingServiceClient.getQuote(any())).willReturn(QUOTE);
        ParcelResponse parcel = parcelService.createCustomerParcel(UUID.randomUUID(), new CreateParcelRequest(
                "123 Main St, Colombo", "COLOMBO_SOUTH", "456 High St, Kandy", "INTER_CITY", "Nimal Perera",
                "+94771234567", ParcelSize.MEDIUM, new BigDecimal("2.50"), DeliverySpeed.STANDARD, "DROP180"));
        UUID courierId = UUID.randomUUID();
        parcelService.updateParcelStatus(parcel.id(), new UpdateParcelStatusRequest(ParcelStatus.PICKED_UP,
                courierId, "Picked up in Colombo", null, null, courierId, "COURIER"));

        // The relay sends one event per parcel per run, oldest first
        outboxRelay.publishPendingEvents();
        outboxRelay.publishPendingEvents();

        List<UUID> eventIds = outboxEventRepository.findByAggregateIdOrderByCreatedAtAsc(parcel.id()).stream()
                .map(OutboxEvent::getEventId)
                .toList();
        assertThat(eventIds).hasSize(2);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(eventIds)
                .allMatch(id -> processedEventRepository.existsByEventIdAndConsumerName(id, QUEUE)));

        List<ParcelStatusChanged> pushed = trackingService.received(eventIds);
        assertThat(pushed).extracting(ParcelStatusChanged::eventId).containsExactlyElementsOf(eventIds);
        assertThat(pushed).allMatch(event -> event.trackingNo().equals(parcel.trackingNo()));
        assertThat(pushed).extracting(ParcelStatusChanged::fromStatus, ParcelStatusChanged::toStatus).containsExactly(
                org.assertj.core.groups.Tuple.tuple(null, ParcelStatus.CREATED),
                org.assertj.core.groups.Tuple.tuple(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
    }

    @Test
    @DisplayName("The same event delivered twice is pushed to the streams once")
    void redeliveredEventIsPushedOnce() {
        ParcelStatusChanged event = outForDelivery();

        rabbitTemplate.convertAndSend(QUEUE, event);
        rabbitTemplate.convertAndSend(QUEUE, event);

        await().atMost(Duration.ofSeconds(15))
                .until(() -> processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), QUEUE));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .until(() -> trackingService.received(List.of(event.eventId())).size() == 1);
        assertThat(processedEventRepository.countByEventIdAndConsumerName(event.eventId(), QUEUE)).isOne();
    }

    @Test
    @DisplayName("A delivery that keeps failing is tried 3 times and dropped, and later events still arrive")
    void failingDeliveryIsRetriedThenDropped() {
        ParcelStatusChanged failing = outForDelivery();
        ParcelStatusChanged next = outForDelivery();
        trackingService.failFor(failing.eventId());

        rabbitTemplate.convertAndSend(QUEUE, failing);
        rabbitTemplate.convertAndSend(QUEUE, next);

        await().atMost(Duration.ofSeconds(20))
                .until(() -> processedEventRepository.existsByEventIdAndConsumerName(next.eventId(), QUEUE));
        assertThat(trackingService.received(List.of(failing.eventId()))).hasSize(3);
        assertThat(processedEventRepository.existsByEventIdAndConsumerName(failing.eventId(), QUEUE)).isFalse();
    }

    private static ParcelStatusChanged outForDelivery() {
        return ParcelStatusChanged.of(Instant.now(), UUID.randomUUID(), "SD-2345-6789", UUID.randomUUID(), null,
                UUID.randomUUID(), ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY, "Out for delivery", null);
    }

    /** Picked up automatically because it is nested in the test class. */
    @TestConfiguration
    static class RecordingTrackingConfig {

        @Bean
        @Primary
        RecordingTrackingService recordingTrackingService(TrackingSseProperties properties) {
            return new RecordingTrackingService(properties);
        }
    }

    /** Records every push, and fails pushes of chosen events, without Mockito (thread-safe). */
    static class RecordingTrackingService extends TrackingService {

        private final List<ParcelStatusChanged> received = new CopyOnWriteArrayList<>();
        private final Set<UUID> failing = ConcurrentHashMap.newKeySet();

        RecordingTrackingService(TrackingSseProperties properties) {
            super(properties);
        }

        @Override
        public void sendUpdate(ParcelStatusChanged event) {
            received.add(event);
            if (failing.contains(event.eventId())) {
                throw new IllegalStateException("push failed");
            }
            super.sendUpdate(event);
        }

        void failFor(UUID eventId) {
            failing.add(eventId);
        }

        /** Pushes of the given events, in the order they happened. */
        List<ParcelStatusChanged> received(List<UUID> eventIds) {
            return received.stream().filter(event -> eventIds.contains(event.eventId())).toList();
        }
    }
}
