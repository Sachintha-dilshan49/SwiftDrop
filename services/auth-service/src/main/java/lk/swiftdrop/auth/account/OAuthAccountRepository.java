package lk.swiftdrop.auth.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, UUID> {

    Optional<OAuthAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
