package lk.swiftdrop.merchant.repository;

import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.MerchantOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantOrderRepository extends JpaRepository<MerchantOrder, UUID> {

    Page<MerchantOrder> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    Optional<MerchantOrder> findByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<MerchantOrder> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    Optional<MerchantOrder> findByTrackingNo(String trackingNo);
}
