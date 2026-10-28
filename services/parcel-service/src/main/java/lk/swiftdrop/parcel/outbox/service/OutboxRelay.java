package lk.swiftdrop.parcel.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.outbox.config.OutboxProperties;
import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import lk.swiftdrop.parcel.outbox.entity.OutboxStatus;
import lk.swiftdrop.parcel.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes outbox rows to RabbitMQ (transactional outbox, docs/architecture.md §6).
 *
 * <ul>
 *   <li>At-least-once: a row becomes {@code SENT} only after the broker confirms it and did not
 *       return it as unroutable. Consumers deduplicate on {@code eventId}.</li>
 *   <li>Transient failures (broker down, nack, missing confirm, returned message) are retried
 *       with exponential backoff and never given up on.</li>
 *   <li>Only the oldest PENDING event of each parcel is picked, and rows locked by another relay are
 *       skipped, so a parcel's events go out in {@code seq} order even with several relays. A failing
 *       event holds back that parcel's later events but not other parcels.</li>
 *   <li>A PARKED event is never published and does not hold back later events.</li>
 * </ul>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                       RabbitTemplate rabbitTemplate,
                       ObjectMapper objectMapper,
                       OutboxProperties properties,
                       Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${swiftdrop.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        Instant now = clock.instant();
        List<OutboxEvent> dueEvents = outboxEventRepository.lockNextDueEvents(now, properties.batchSize());
        if (dueEvents.isEmpty()) {
            return;
        }

        List<InFlight> inFlight = new ArrayList<>();
        for (OutboxEvent event : dueEvents) {
            ParcelStatusChanged payload;
            try {
                payload = objectMapper.readValue(event.getPayload(), ParcelStatusChanged.class);
            } catch (JsonProcessingException e) {
                park(event, "Unreadable payload: " + e.getOriginalMessage());
                continue;
            }

            CorrelationData correlation = new CorrelationData(event.getEventId().toString());
            try {
                rabbitTemplate.convertAndSend(ParcelEvents.EXCHANGE, event.getRoutingKey(), payload, message -> {
                    message.getMessageProperties().setMessageId(event.getEventId().toString());
                    return message;
                }, correlation);
                inFlight.add(new InFlight(event, correlation));
            } catch (AmqpException e) {
                scheduleRetry(event, now, "Publish failed: " + e.getMessage());
            }
        }

        long deadline = System.nanoTime() + properties.confirmTimeout().toNanos();
        for (InFlight sent : inFlight) {
            String failure = awaitConfirm(sent.correlation(), deadline);
            if (failure == null) {
                markSent(sent.event(), now);
            } else {
                scheduleRetry(sent.event(), now, failure);
            }
        }

        outboxEventRepository.saveAll(dueEvents);
    }

    /** Returns {@code null} when the broker accepted and routed the message, otherwise why not. */
    private String awaitConfirm(CorrelationData correlation, long deadlineNanos) {
        CorrelationData.Confirm confirm;
        try {
            long remaining = Math.max(0, deadlineNanos - System.nanoTime());
            confirm = correlation.getFuture().get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            return "No publisher confirm within " + properties.confirmTimeout();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted while waiting for publisher confirm";
        } catch (ExecutionException e) {
            return "Publisher confirm failed: " + e.getCause().getMessage();
        }

        if (!confirm.ack()) {
            return "Broker nacked the message: " + confirm.reason();
        }
        ReturnedMessage returned = correlation.getReturned();
        if (returned != null) {
            return "Broker returned the message as unroutable: " + returned.getReplyText();
        }
        return null;
    }

    private void markSent(OutboxEvent event, Instant now) {
        event.setStatus(OutboxStatus.SENT);
        event.setSentAt(now);
        event.setLastError(null);
        log.info("Relayed outbox event {} to exchange {} with key {}",
                event.getEventId(), ParcelEvents.EXCHANGE, event.getRoutingKey());
    }

    private void scheduleRetry(OutboxEvent event, Instant now, String reason) {
        int attempts = event.getRetryCount() + 1;
        Duration delay = backoff(attempts);
        event.setRetryCount(attempts);
        event.setNextAttemptAt(now.plus(delay));
        event.setLastError(truncate(reason));
        if (attempts >= properties.alertAfterAttempts()) {
            log.error("Outbox event {} still unpublished after {} attempts, retrying in {}: {}",
                    event.getEventId(), attempts, delay, reason);
        } else {
            log.warn("Outbox event {} not published (attempt {}), retrying in {}: {}",
                    event.getEventId(), attempts, delay, reason);
        }
    }

    private void park(OutboxEvent event, String reason) {
        event.setStatus(OutboxStatus.PARKED);
        event.setLastError(truncate(reason));
        log.error("Parked outbox event {} of parcel {}; it will not be published, later events continue: {}",
                event.getEventId(), event.getAggregateId(), reason);
    }

    /** 1 s, 2 s, 4 s, ... capped at {@code maxBackoff}. */
    Duration backoff(int attempts) {
        Duration delay = properties.initialBackoff().multipliedBy(1L << Math.min(attempts - 1, 30));
        return delay.compareTo(properties.maxBackoff()) > 0 ? properties.maxBackoff() : delay;
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_ERROR_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_ERROR_LENGTH);
    }

    private record InFlight(OutboxEvent event, CorrelationData correlation) {
    }
}
