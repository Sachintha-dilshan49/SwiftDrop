package lk.swiftdrop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleEntity;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.api.AdminUserUpdateRequest;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void changesRolesAndStatusAndRevokesExistingRefreshTokens() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("Demo User", "demo@example.test", "0771234567", "hash");
        user.addRole(new RoleEntity(RoleName.CUSTOMER));
        RoleEntity merchantRole = new RoleEntity(RoleName.MERCHANT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = adminUserService.updateUser(
                userId,
                new AdminUserUpdateRequest(Set.of(RoleName.MERCHANT), AccountStatus.DISABLED));

        assertThat(response.roles()).containsExactly("MERCHANT");
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(user.getRoles()).containsExactly(merchantRole);
        verify(refreshTokenRepository).revokeAllByUserId(userId, NOW);
    }

    @Test
    void reportsMissingUserForAdminUpdate() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateUser(
                        userId,
                        new AdminUserUpdateRequest(Set.of(RoleName.CUSTOMER), AccountStatus.ACTIVE)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found.");
    }
}
