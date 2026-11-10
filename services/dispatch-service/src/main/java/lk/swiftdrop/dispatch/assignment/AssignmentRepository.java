package lk.swiftdrop.dispatch.assignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    boolean existsByParcelIdAndStatusIn(UUID parcelId, Collection<AssignmentStatus> statuses);

    /** Which of {@code parcelIds} already have an assignment in one of {@code statuses}. */
    @Query("select a.parcelId from Assignment a where a.parcelId in :parcelIds and a.status in :statuses")
    Set<UUID> findParcelIdsWithStatus(@Param("parcelIds") Collection<UUID> parcelIds,
            @Param("statuses") Collection<AssignmentStatus> statuses);

    /** The courier's still-open deliveries plus any that changed since {@code since}, oldest first. */
    @Query("""
            select a from Assignment a
            where a.courierId = :courierId
              and (a.status in :active or a.updatedAt >= :since)
            order by a.assignedAt
            """)
    List<Assignment> findDayList(@Param("courierId") UUID courierId,
            @Param("active") Collection<AssignmentStatus> active, @Param("since") Instant since);
}
