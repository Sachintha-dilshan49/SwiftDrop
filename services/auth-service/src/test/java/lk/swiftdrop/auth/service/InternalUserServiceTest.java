package lk.swiftdrop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleEntity;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private InternalUserService internalUserService;

    @BeforeEach
    void setUp() {
        internalUserService = new InternalUserService(
                userRepository, roleRepository, refreshTokenRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void grantsCourierRoleAndRevokesRefreshTokens() {
        UserEntity user = new UserEntity("Demo User", "demo@example.test", null, "hash");
        RoleEntity courierRole = new RoleEntity(RoleName.COURIER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.COURIER)).thenReturn(Optional.of(courierRole));

        internalUserService.grantCourierRole(USER_ID);

        assertThat(user.getRoles()).contains(courierRole);
        verify(userRepository).saveAndFlush(user);
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID, NOW);
    }

    @Test
    void repeatedGrantDoesNotRevokeTokensAgain() {
        UserEntity user = new UserEntity("Demo User", "demo@example.test", null, "hash");
        user.addRole(new RoleEntity(RoleName.COURIER));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        internalUserService.grantCourierRole(USER_ID);

        verify(roleRepository, never()).findByName(RoleName.COURIER);
        verify(userRepository, never()).saveAndFlush(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    void doesNotGrantCourierRoleToInactiveAccount() {
        UserEntity user = new UserEntity("Demo User", "demo@example.test", null, "hash");
        user.setAccountStatus(AccountStatus.DISABLED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> internalUserService.grantCourierRole(USER_ID))
                .isInstanceOf(ConflictException.class);
        verify(roleRepository, never()).findByName(RoleName.COURIER);
    }

    @Test
    void reportsUnknownAccount() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalUserService.grantCourierRole(USER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void grantsMerchantRoleAndRevokesRefreshTokens() {
        UserEntity user = new UserEntity("Demo User", "demo@example.test", null, "hash");
        RoleEntity merchantRole = new RoleEntity(RoleName.MERCHANT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));

        internalUserService.grantMerchantRole(USER_ID);

        assertThat(user.getRoles()).contains(merchantRole);
        verify(userRepository).saveAndFlush(user);
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID, NOW);
    }

    @Test
    void doesNotGrantMerchantRoleToInactiveAccount() {
        UserEntity user = new UserEntity("Demo User", "demo@example.test", null, "hash");
        user.setAccountStatus(AccountStatus.DISABLED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> internalUserService.grantMerchantRole(USER_ID))
                .isInstanceOf(ConflictException.class);
        verify(roleRepository, never()).findByName(RoleName.MERCHANT);
    }
}
