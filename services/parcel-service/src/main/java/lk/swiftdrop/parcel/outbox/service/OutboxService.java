package lk.swiftdrop.parcel.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import lk.swiftdrop.parcel.outbox.repository.OutboxEventRepository;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueues a status change event into the outbox within the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueStatusChangedEvent(Parcel parcel, ParcelStatus fromStatus, String note) {
        ParcelStatusChanged event = ParcelStatusChanged.of(
                Instant.now(),
                parcel.getId(),
                parcel.getTrackingNo(),
                parcel.getCustomerId(),
                parcel.getMerchantId(),
                parcel.getCourierId(),
                fromStatus,
                parcel.getStatus(),
                note,
                parcel.getFailureReason()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            String routingKey = ParcelEvents.routingKey(parcel.getStatus());

            OutboxEvent outboxEvent = new OutboxEvent(
                    UUID.randomUUID(),
                    event.eventId(),
                    parcel.getId(),
                    ParcelStatusChanged.TYPE,
                    routingKey,
                    payload
            );

            outboxEventRepository.save(outboxEvent);
            log.info("Enqueued outbox event {} for parcel {} moving from {} to {}",
                    event.eventId(), parcel.getId(), fromStatus, parcel.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ParcelStatusChanged event for parcel {}", parcel.getId(), e);
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}
