package lk.swiftdrop.dispatch.courier;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    Optional<Courier> findByUserId(UUID userId);
}
