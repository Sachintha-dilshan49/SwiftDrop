package lk.swiftdrop.dispatch.repository;

import java.util.UUID;
import lk.swiftdrop.dispatch.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
}
