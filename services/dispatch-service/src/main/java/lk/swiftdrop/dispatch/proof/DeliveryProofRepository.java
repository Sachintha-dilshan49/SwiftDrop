package lk.swiftdrop.dispatch.proof;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID> {

    List<DeliveryProof> findByAssignmentId(UUID assignmentId);
}
