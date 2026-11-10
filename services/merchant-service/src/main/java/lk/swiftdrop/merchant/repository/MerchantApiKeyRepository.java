package lk.swiftdrop.merchant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.merchant.entity.MerchantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, UUID> {

    List<MerchantApiKey> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<MerchantApiKey> findByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<MerchantApiKey> findByKeyHash(String keyHash);

    Optional<MerchantApiKey> findByKeyHashAndRevokedFalse(String keyHash);
}
