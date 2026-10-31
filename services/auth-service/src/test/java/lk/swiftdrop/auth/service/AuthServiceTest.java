package lk.swiftdrop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import lk.swiftdrop.auth.api.LoginRequest;
import lk.swiftdrop.auth.api.SignupRequest;
import lk.swiftdrop.auth.api.UserContactResponse;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.RefreshTokenEntity;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.auth.token.TokenResponse;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordEncoder,
                tokenService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void signsUpCustomerWithEncodedPasswordAndReturnsTokens() {
        RoleEntity customer = new RoleEntity(RoleName.CUSTOMER);
        when(userRepository.existsByEmailIgnoreCase("demo@example.test")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("long-enough-password")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubTokenIssue();

        TokenResponse response = authService.signup(new SignupRequest(
                " Demo Customer ", " DEMO@example.test ", " 0771234567 ", "long-enough-password"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("signed-access");
        assertThat(response.accessTokenExpiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(response.user().email()).isEqualTo("demo@example.test");
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Demo Customer");
        assertThat(userCaptor.getValue().getMobileNumber()).isEqualTo("0771234567");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void rejectsDuplicateSignupWithoutSavingUser() {
        when(userRepository.existsByEmailIgnoreCase("demo@example.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("Demo Customer", "demo@example.test", null, "long-enough-password")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An account with this email or mobile number already exists.");
    }

    @Test
    void rejectsSignupWhenUniqueEmailConstraintWinsRace() {
        when(userRepository.existsByEmailIgnoreCase("demo@example.test")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(new RoleEntity(RoleName.CUSTOMER)));
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("Demo Customer", "demo@example.test", null, "long-enough-password")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An account with this email or mobile number already exists.");
    }

    @Test
    void logsInActiveUserAndIssuesNewRefreshToken() {
        UserEntity user = customerUser();
        when(userRepository.findByEmailIgnoreCase("demo@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("long-enough-password", "stored-hash")).thenReturn(true);
        stubTokenIssue();

        TokenResponse response = authService.login(new LoginRequest(" demo@example.test ", "long-enough-password"));

        assertThat(response.refreshToken()).isEqualTo("raw-refresh");
        verify(passwordEncoder).matches("long-enough-password", "stored-hash");
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void rejectsInvalidCredentialsAndDisabledAccounts() {
        when(userRepository.findByEmailIgnoreCase("demo@example.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("demo@example.test", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email or mobile number and password are invalid.");

        UserEntity disabled = customerUser();
        disabled.disable();
        when(userRepository.findByEmailIgnoreCase("disabled@example.test")).thenReturn(Optional.of(disabled));
        assertThatThrownBy(() -> authService.login(new LoginRequest("disabled@example.test", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logsInWithNormalizedMobileNumber() {
        UserEntity user = customerUser();
        when(userRepository.findByEmailIgnoreCase("+94 77-123-4567")).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber("+94771234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("long-enough-password", "stored-hash")).thenReturn(true);
        stubTokenIssue();

        TokenResponse response = authService.login(new LoginRequest("+94 77-123-4567", "long-enough-password"));

        assertThat(response.user().email()).isEqualTo("demo@example.test");
        verify(userRepository).findByMobileNumber("+94771234567");
    }

    @Test
    void returnsContactDetailsForInternalLookup() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("Demo Customer", "demo@example.test", "0771234567", "stored-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserContactResponse contact = authService.getUserContact(userId);

        assertThat(contact.email()).isEqualTo("demo@example.test");
        assertThat(contact.phone()).isEqualTo("0771234567");
        assertThat(contact.firstName()).isEqualTo("Demo");
    }

    @Test
    void reportsMissingUserForInternalContactLookup() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserContact(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found.");
    }

    @Test
    void rotatesRefreshTokenAndRevokesPreviousToken() {
        UserEntity user = customerUser();
        RefreshTokenEntity oldToken =
                new RefreshTokenEntity(user, "old-hash", NOW.plusSeconds(120));
        when(tokenService.hashRefreshToken("raw-old")).thenReturn("old-hash");
        when(tokenService.createRefreshToken()).thenReturn("raw-new");
        when(tokenService.hashRefreshToken("raw-new")).thenReturn("new-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubAccessToken();

        TokenResponse response = authService.refresh("raw-old");

        assertThat(response.refreshToken()).isEqualTo("raw-new");
        assertThat(oldToken.getRevokedAt()).isEqualTo(NOW);
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void rejectsUnknownExpiredRevokedAndDisabledRefreshTokens() {
        when(tokenService.hashRefreshToken("missing")).thenReturn("missing-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("missing-hash")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refresh("missing"))
                .isInstanceOf(UnauthorizedException.class);

        UserEntity user = customerUser();
        RefreshTokenEntity expired = new RefreshTokenEntity(user, "expired", NOW);
        when(tokenService.hashRefreshToken("expired")).thenReturn("expired");
        when(refreshTokenRepository.findByTokenHashForUpdate("expired")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> authService.refresh("expired"))
                .isInstanceOf(UnauthorizedException.class);

        RefreshTokenEntity revoked = new RefreshTokenEntity(user, "revoked", NOW.plusSeconds(100));
        revoked.revoke(NOW.minusSeconds(1), null);
        when(tokenService.hashRefreshToken("revoked")).thenReturn("revoked");
        when(refreshTokenRepository.findByTokenHashForUpdate("revoked")).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> authService.refresh("revoked"))
                .isInstanceOf(UnauthorizedException.class);

        UserEntity disabled = customerUser();
        disabled.disable();
        RefreshTokenEntity disabledToken = new RefreshTokenEntity(disabled, "disabled", NOW.plusSeconds(100));
        when(tokenService.hashRefreshToken("disabled")).thenReturn("disabled");
        when(refreshTokenRepository.findByTokenHashForUpdate("disabled")).thenReturn(Optional.of(disabledToken));
        assertThatThrownBy(() -> authService.refresh("disabled"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutRevokesKnownTokenButIgnoresUnknownAndAlreadyRevokedTokens() {
        UserEntity user = customerUser();
        RefreshTokenEntity token = new RefreshTokenEntity(user, "known", NOW.plusSeconds(100));
        when(tokenService.hashRefreshToken("known")).thenReturn("known");
        when(refreshTokenRepository.findByTokenHash("known")).thenReturn(Optional.of(token));
        authService.logout("known");
        assertThat(token.getRevokedAt()).isEqualTo(NOW);

        RefreshTokenEntity alreadyRevoked = new RefreshTokenEntity(user, "revoked", NOW.plusSeconds(100));
        alreadyRevoked.revoke(NOW.minusSeconds(1), null);
        when(tokenService.hashRefreshToken("revoked")).thenReturn("revoked");
        when(refreshTokenRepository.findByTokenHash("revoked")).thenReturn(Optional.of(alreadyRevoked));
        authService.logout("revoked");
        assertThat(alreadyRevoked.getRevokedAt()).isEqualTo(NOW.minusSeconds(1));

        when(tokenService.hashRefreshToken("unknown")).thenReturn("unknown");
        when(refreshTokenRepository.findByTokenHash("unknown")).thenReturn(Optional.empty());
        authService.logout("unknown");
    }

    private void stubTokenIssue() {
        when(tokenService.createRefreshToken()).thenReturn("raw-refresh");
        when(tokenService.hashRefreshToken("raw-refresh")).thenReturn("hashed-refresh");
        stubAccessToken();
    }

    private void stubAccessToken() {
        when(tokenService.createAccessToken(any(UserEntity.class))).thenReturn("signed-access");
    }

    private static UserEntity customerUser() {
        UserEntity user = new UserEntity("Demo Customer", "demo@example.test", null, "stored-hash");
        user.addRole(new RoleEntity(RoleName.CUSTOMER));
        return user;
    }
}
