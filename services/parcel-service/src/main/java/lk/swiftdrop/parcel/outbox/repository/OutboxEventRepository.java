package lk.swiftdrop.parcel.outbox.repository;

import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Locks the next events that are due, taking only the oldest PENDING event of each parcel, so a
     * parcel's events are published in {@code seq} order. An earlier event that is waiting for a retry
     * (or locked by another relay) holds back the parcel's later events; a PARKED event does not.
     * Rows locked by another relay are skipped, and at most one event per parcel is returned.
     */
    @Query(value = """
            SELECT o.* FROM outbox_events o
            WHERE o.status = 'PENDING'
              AND o.next_attempt_at <= :now
              AND NOT EXISTS (
                  SELECT 1 FROM outbox_events earlier
                  WHERE earlier.aggregate_id = o.aggregate_id
                    AND earlier.status = 'PENDING'
                    AND earlier.seq < o.seq)
            ORDER BY o.seq
            LIMIT :limit
            FOR UPDATE OF o SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextDueEvents(@Param("now") Instant now, @Param("limit") int limit);

    List<OutboxEvent> findByAggregateIdOrderByCreatedAtAsc(UUID aggregateId);
}
