package lk.swiftdrop.dispatch.courier;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierRepository extends JpaRepository<Courier, UUID> {
}
