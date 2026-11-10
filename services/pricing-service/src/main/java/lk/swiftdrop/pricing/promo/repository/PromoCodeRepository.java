package lk.swiftdrop.pricing.promo.repository;

import lk.swiftdrop.pricing.promo.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCode(String code);

    boolean existsByCode(String code);

    List<PromoCode> findAllByOrderByCodeAsc();
}
