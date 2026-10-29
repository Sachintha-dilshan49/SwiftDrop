package lk.swiftdrop.parcel.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import lk.swiftdrop.parcel.outbox.entity.OutboxStatus;
import lk.swiftdrop.parcel.outbox.repository.OutboxEventRepository;
import lk.swiftdrop.parcel.outbox.service.OutboxRelay;
import lk.swiftdrop.parcel.outbox.service.OutboxService;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.support.AbstractContainersIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Outbox relay against a real broker and database: publisher confirms, returned messages, and
 * per-parcel ordering with retries, parked events and several relays at once.
 */
class OutboxRelayIT extends AbstractContainersIT {

    private static final String TEST_QUEUE = "outbox-relay-it.q";
    private static final String UNROUTABLE_KEY = "outbox.it.unroutable";

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper eventObjectMapper;

    private final TopicExchange exchange = new TopicExchange(ParcelEvents.EXCHANGE, true, false);
    private final Queue testQueue = QueueBuilder.durable(TEST_QUEUE).build();

    @BeforeEach
    void setUp() {
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(testQueue);
        amqpAdmin.declareBinding(BindingBuilder.bind(testQueue).to(exchange).with(ParcelEvents.ALL_STATUS_CHANGES));
        amqpAdmin.removeBinding(unroutableBinding());
        amqpAdmin.purgeQueue(TEST_QUEUE);
    }

    @Test
    @DisplayName("A confirmed event is marked SENT and arrives with messageId = eventId")
    void confirmedEventIsMarkedSent() {
        OutboxEvent event = insertEvent(UUID.randomUUID(), OutboxStatus.PENDING, Instant.now());

        outboxRelay.publishPendingEvents();

        OutboxEvent stored = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(stored.getSentAt()).isNotNull();
        assertThat(receiveInOrder(Set.of(event.getEventId()))).containsExactly(event.getEventId());
    }

    @Test
    @DisplayName("An unroutable event is returned, stays PENDING, and is sent once a queue is bound")
    void returnedEventIsRetriedUntilRoutable() {
        OutboxEvent event = insertEvent(UUID.randomUUID(), UNROUTABLE_KEY, OutboxStatus.PENDING, Instant.now());

        outboxRelay.publishPendingEvents();

        OutboxEvent afterFirstRun = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(afterFirstRun.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(afterFirstRun.getRetryCount()).isEqualTo(1);
        assertThat(afterFirstRun.getLastError()).contains("unroutable");
        assertThat(afterFirstRun.getNextAttemptAt()).isAfter(Instant.now());

        amqpAdmin.declareBinding(unroutableBinding());
        makeDue(event);
        outboxRelay.publishPendingEvents();

        OutboxEvent afterSecondRun = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(afterSecondRun.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(afterSecondRun.getLastError()).isNull();
    }

    @Test
    @DisplayName("Several pending events of one parcel are published one per run, oldest first, and arrive in that order")
    void pendingEventsOfOneParcelArePublishedInOrder() {
        UUID parcel = UUID.randomUUID();
        OutboxEvent first = insertEvent(parcel, OutboxStatus.PENDING, Instant.now());
        OutboxEvent second = insertEvent(parcel, OutboxStatus.PENDING, Instant.now());
        OutboxEvent third = insertEvent(parcel, OutboxStatus.PENDING, Instant.now());

        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(first, second, third), OutboxStatus.SENT, OutboxStatus.PENDING, OutboxStatus.PENDING);

        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(first, second, third), OutboxStatus.SENT, OutboxStatus.SENT, OutboxStatus.PENDING);

        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(first, second, third), OutboxStatus.SENT, OutboxStatus.SENT, OutboxStatus.SENT);

        assertThat(receiveInOrder(Set.of(first.getEventId(), second.getEventId(), third.getEventId())))
                .containsExactly(first.getEventId(), second.getEventId(), third.getEventId());
    }

    @Test
    @DisplayName("An earlier event waiting for a retry holds back its parcel's later event, but not other parcels")
    void retryHoldsBackOnlyItsOwnParcel() {
        UUID parcelA = UUID.randomUUID();
        UUID parcelB = UUID.randomUUID();
        OutboxEvent firstOfA = insertEvent(parcelA, OutboxStatus.PENDING, Instant.now().plusSeconds(3_600));
        OutboxEvent secondOfA = insertEvent(parcelA, OutboxStatus.PENDING, Instant.now());
        OutboxEvent onlyOfB = insertEvent(parcelB, OutboxStatus.PENDING, Instant.now());

        outboxRelay.publishPendingEvents();

        assertStatuses(List.of(firstOfA, secondOfA, onlyOfB), OutboxStatus.PENDING, OutboxStatus.PENDING, OutboxStatus.SENT);

        makeDue(firstOfA);
        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(firstOfA, secondOfA), OutboxStatus.SENT, OutboxStatus.PENDING);

        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(firstOfA, secondOfA), OutboxStatus.SENT, OutboxStatus.SENT);
    }

    @Test
    @DisplayName("A PARKED event is not published and does not hold back the parcel's newer pending event")
    void parkedEventDoesNotHoldBackLaterEvents() {
        UUID parcel = UUID.randomUUID();
        OutboxEvent parked = insertEvent(parcel, OutboxStatus.PARKED, Instant.now());
        OutboxEvent newer = insertEvent(parcel, OutboxStatus.PENDING, Instant.now());

        outboxRelay.publishPendingEvents();

        assertStatuses(List.of(parked, newer), OutboxStatus.PARKED, OutboxStatus.SENT);
        assertThat(receiveInOrder(Set.of(parked.getEventId(), newer.getEventId())))
                .containsExactly(newer.getEventId());
    }

    @Test
    @DisplayName("With two relays at once, an event locked by one is skipped by the other and its parcel's later event waits")
    void concurrentRelaysKeepPerParcelOrder() throws Exception {
        UUID parcelA = UUID.randomUUID();
        UUID parcelB = UUID.randomUUID();
        OutboxEvent firstOfA = insertEvent(parcelA, OutboxStatus.PENDING, Instant.now());
        OutboxEvent secondOfA = insertEvent(parcelA, OutboxStatus.PENDING, Instant.now());
        OutboxEvent onlyOfB = insertEvent(parcelB, OutboxStatus.PENDING, Instant.now());

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService otherRelay = Executors.newSingleThreadExecutor();
        try {
            // The other relay holds the row lock on A's first event, as it does while publishing it
            Future<?> holder = otherRelay.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbcTemplate.queryForList("SELECT id FROM outbox_events WHERE id = ? FOR UPDATE", firstOfA.getId());
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            outboxRelay.publishPendingEvents();

            assertStatuses(List.of(firstOfA, secondOfA, onlyOfB), OutboxStatus.PENDING, OutboxStatus.PENDING, OutboxStatus.SENT);

            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            otherRelay.shutdownNow();
        }

        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(firstOfA, secondOfA), OutboxStatus.SENT, OutboxStatus.PENDING);

        outboxRelay.publishPendingEvents();
        assertStatuses(List.of(firstOfA, secondOfA), OutboxStatus.SENT, OutboxStatus.SENT);

        assertThat(receiveInOrder(Set.of(firstOfA.getEventId(), secondOfA.getEventId())))
                .containsExactly(firstOfA.getEventId(), secondOfA.getEventId());
    }

    @Test
    @DisplayName("Outbox rows can only be written inside the transaction of the parcel change")
    void outboxWriteRequiresSurroundingTransaction() {
        Parcel parcel = new Parcel(UUID.randomUUID(), "SD-7K3Q-9P2M", UUID.randomUUID(), null, "Pickup", "Dropoff",
                "Recipient", "+94771234567", ParcelSize.SMALL, BigDecimal.ONE, new BigDecimal("350.00"), "LKR");

        assertThatThrownBy(() -> outboxService.enqueueStatusChangedEvent(parcel, null, "Created"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private OutboxEvent insertEvent(UUID parcelId, OutboxStatus status, Instant nextAttemptAt) {
        return insertEvent(parcelId, ParcelEvents.routingKey(ParcelStatus.CREATED), status, nextAttemptAt);
    }

    /** Each insert is its own transaction, so seq follows insertion order. */
    private OutboxEvent insertEvent(UUID parcelId, String routingKey, OutboxStatus status, Instant nextAttemptAt) {
        try {
            ParcelStatusChanged event = ParcelStatusChanged.of(Instant.now(), parcelId, "SD-2345-6789",
                    UUID.randomUUID(), null, null, null, ParcelStatus.CREATED, "Created", null);
            OutboxEvent row = new OutboxEvent(UUID.randomUUID(), event.eventId(), parcelId, ParcelStatusChanged.TYPE,
                    routingKey, eventObjectMapper.writeValueAsString(event));
            row.setStatus(status);
            row.setNextAttemptAt(nextAttemptAt);
            return outboxEventRepository.save(row);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void makeDue(OutboxEvent event) {
        jdbcTemplate.update("UPDATE outbox_events SET next_attempt_at = now() - interval '1 second' WHERE id = ?",
                event.getId());
    }

    private void assertStatuses(List<OutboxEvent> events, OutboxStatus... expected) {
        List<OutboxStatus> actual = events.stream()
                .map(event -> outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
                .toList();
        assertThat(actual).containsExactly(expected);
    }

    /** Event IDs from {@code wanted} in the order they arrived on the test queue; other tests' events are skipped. */
    private List<UUID> receiveInOrder(Set<UUID> wanted) {
        List<UUID> received = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 3_000;
        while (received.size() < wanted.size() && System.currentTimeMillis() < deadline) {
            Message message = rabbitTemplate.receive(TEST_QUEUE, 500);
            if (message != null) {
                UUID eventId = UUID.fromString(message.getMessageProperties().getMessageId());
                if (wanted.contains(eventId)) {
                    received.add(eventId);
                }
            }
        }
        return received;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Binding unroutableBinding() {
        return BindingBuilder.bind(testQueue).to(exchange).with(UNROUTABLE_KEY);
    }
}
