package lk.swiftdrop.parcel.tracking.repository;

import lk.swiftdrop.parcel.tracking.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);

    long countByEventIdAndConsumerName(UUID eventId, String consumerName);
}
