package lk.swiftdrop.auth.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByMobileNumber(String mobileNumber);

    @Modifying
    @Query("update UserEntity user set user.passwordResetEmail = :email where user.passwordResetEmail is null")
    int fillMissingPasswordResetEmail(@Param("email") String email);
}
