package lk.swiftdrop.parcel.tracking.consumer;

import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.tracking.entity.ProcessedEvent;
import lk.swiftdrop.parcel.tracking.repository.ProcessedEventRepository;
import lk.swiftdrop.parcel.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes {@code ParcelStatusChanged} from {@code tracking.q} and pushes each change to the open live
 * tracking streams (docs/architecture.md §6.3, diagram seq-live-tracking).
 *
 * <p>Idempotent (AGENTS.md §5.7): an {@code eventId} already stored in {@code processed_events} for this
 * queue is skipped. The row is flushed before the push, so if two deliveries of one event race, the
 * unique key fails for the second before it reaches any stream; its retry then sees the stored row.
 * A delivery that keeps failing is retried and then dropped (see {@code spring.rabbitmq.listener}).
 *
 * <p>Streams live in memory on one instance. With more than one parcel-service replica, each replica would
 * need its own queue bound to {@code parcel.events}, because a shared {@code tracking.q} hands each event
 * to only one replica. Docker Compose runs one replica. Duplicates are tracked per queue name, which
 * also fits one queue per replica.
 */
@Component
public class TrackingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrackingEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final TrackingService trackingService;
    private final String consumerName;

    public TrackingEventConsumer(ProcessedEventRepository processedEventRepository,
                                 TrackingService trackingService,
                                 @Value("${rabbitmq.queues.tracking:tracking.q}") String queueName) {
        this.processedEventRepository = processedEventRepository;
        this.trackingService = trackingService;
        this.consumerName = queueName;
    }

    @RabbitListener(queues = "${rabbitmq.queues.tracking:tracking.q}")
    @Transactional
    public void onParcelStatusChanged(ParcelStatusChanged event) {
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), consumerName)) {
            log.debug("Skipping duplicate event {}", event.eventId());
            return;
        }
        processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId(), consumerName));
        log.debug("Pushing event {} ({}) to live tracking streams", event.eventId(), event.toStatus());
        trackingService.sendUpdate(event);
    }
}
