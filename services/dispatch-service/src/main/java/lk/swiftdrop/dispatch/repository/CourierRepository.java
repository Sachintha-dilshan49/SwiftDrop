package lk.swiftdrop.dispatch.repository;

import java.util.UUID;
import lk.swiftdrop.dispatch.entity.Courier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierRepository extends JpaRepository<Courier, UUID> {
}
