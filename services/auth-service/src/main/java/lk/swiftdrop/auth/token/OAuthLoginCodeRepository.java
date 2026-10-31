package lk.swiftdrop.auth.token;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCodeEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from OAuthLoginCodeEntity code join fetch code.user where code.codeHash = :codeHash")
    Optional<OAuthLoginCodeEntity> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
