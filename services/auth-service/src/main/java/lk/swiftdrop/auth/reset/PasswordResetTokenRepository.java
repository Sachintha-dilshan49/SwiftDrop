package lk.swiftdrop.auth.reset;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetTokenEntity token join fetch token.user where token.tokenHash = :tokenHash")
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /** True when this user was sent a link that is still unused since {@code since} (resend throttle). */
    @Query("select count(token) > 0 from PasswordResetTokenEntity token "
            + "where token.user.id = :userId and token.usedAt is null and token.createdAt > :since")
    boolean existsUnusedSince(@Param("userId") UUID userId, @Param("since") Instant since);

    /** Older links stop working once a new one is sent or the password changes. */
    @Modifying
    @Query("update PasswordResetTokenEntity token set token.usedAt = :now "
            + "where token.user.id = :userId and token.usedAt is null")
    int invalidateAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
