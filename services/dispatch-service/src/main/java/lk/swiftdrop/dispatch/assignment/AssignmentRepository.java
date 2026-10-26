package lk.swiftdrop.dispatch.assignment;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    boolean existsByParcelIdAndStatusIn(UUID parcelId, Collection<AssignmentStatus> statuses);
}
