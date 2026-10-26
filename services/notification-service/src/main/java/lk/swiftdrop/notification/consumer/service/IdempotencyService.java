package lk.swiftdrop.notification.consumer.service;

import lk.swiftdrop.notification.consumer.entity.ProcessedEvent;
import lk.swiftdrop.notification.consumer.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final ProcessedEventRepository repository;

    public IdempotencyService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    public boolean isAlreadyProcessed(UUID eventId, String consumerName) {
        return repository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    @Transactional
    public void markProcessed(UUID eventId, String consumerName) {
        if (repository.existsByEventIdAndConsumerName(eventId, consumerName)) {
            log.debug("Event {} already marked for consumer {}, skipping", eventId, consumerName);
            return;
        }
        repository.save(new ProcessedEvent(eventId, consumerName));
        log.debug("Marked event {} as processed for consumer {}", eventId, consumerName);
    }
}
