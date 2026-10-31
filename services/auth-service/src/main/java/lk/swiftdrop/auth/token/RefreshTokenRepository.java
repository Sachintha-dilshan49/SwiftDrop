package lk.swiftdrop.auth.token;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token join fetch token.user where token.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}
