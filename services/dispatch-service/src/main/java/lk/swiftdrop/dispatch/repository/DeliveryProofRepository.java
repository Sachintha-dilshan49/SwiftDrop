package lk.swiftdrop.dispatch.repository;

import java.util.UUID;
import lk.swiftdrop.dispatch.entity.DeliveryProof;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID> {
}
