package lk.swiftdrop.merchant.repository;

import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.MerchantWebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantWebhookEndpointRepository extends JpaRepository<MerchantWebhookEndpoint, UUID> {

    Optional<MerchantWebhookEndpoint> findByMerchantId(UUID merchantId);

    boolean existsByMerchantId(UUID merchantId);
}
