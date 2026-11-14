package lk.swiftdrop.auth.service;

import java.time.Clock;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public InternalUserService(UserRepository userRepository, RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public void grantCourierRole(UUID userId) {
        grantRole(userId, RoleName.COURIER);
    }

    @Transactional
    public void grantMerchantRole(UUID userId) {
        grantRole(userId, RoleName.MERCHANT);
    }

    private void grantRole(UUID userId, RoleName roleName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("account-not-active", "Account is not active",
                    "A role cannot be granted to an inactive account.");
        }
        boolean alreadyAssigned = user.getRoles().stream()
                .anyMatch(role -> role.getName() == roleName);
        if (!alreadyAssigned) {
            var role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException(
                            "Required " + roleName + " role is missing from auth_db."));
            user.addRole(role);
            userRepository.saveAndFlush(user);
            refreshTokenRepository.revokeAllByUserId(userId, clock.instant());
        }
    }
}
