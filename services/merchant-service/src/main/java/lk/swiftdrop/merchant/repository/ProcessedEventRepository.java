package lk.swiftdrop.merchant.repository;

import java.util.UUID;
import lk.swiftdrop.merchant.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);
}
