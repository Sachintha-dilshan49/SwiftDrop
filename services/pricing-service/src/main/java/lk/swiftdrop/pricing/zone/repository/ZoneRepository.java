package lk.swiftdrop.pricing.zone.repository;

import lk.swiftdrop.pricing.zone.entity.Zone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    Optional<Zone> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "districts")
    List<Zone> findByActiveTrue();

    @EntityGraph(attributePaths = "districts")
    List<Zone> findAllBy();

    @EntityGraph(attributePaths = "districts")
    Optional<Zone> findWithDistrictsById(UUID id);
}
