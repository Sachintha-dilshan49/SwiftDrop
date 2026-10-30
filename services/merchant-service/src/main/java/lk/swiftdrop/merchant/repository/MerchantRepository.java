package lk.swiftdrop.merchant.repository;

import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByUserId(UUID userId);

    Optional<Merchant> findByBusinessRegNo(String businessRegNo);

    Optional<Merchant> findByEmail(String email);

    boolean existsByBusinessRegNo(String businessRegNo);

    boolean existsByEmail(String email);

    boolean existsByUserId(UUID userId);

    Page<Merchant> findAllByStatus(MerchantStatus status, Pageable pageable);
}
