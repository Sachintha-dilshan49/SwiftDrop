package lk.swiftdrop.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T08:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService tokenService;
    @Mock private ApplicationEventPublisher events;

    private PasswordResetService service(String defaultRecipient) {
        return new PasswordResetService(userRepository, resetTokenRepository, refreshTokenRepository,
                passwordEncoder, tokenService, events,
                new PasswordResetProperties("http://localhost:3000/reset-password", null, defaultRecipient),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity("Nimal Perera", "nimal@example.test", null, "old-hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    @Test
    void forgotPasswordEmailsSingleUseLinkToAccountEmail() {
        UserEntity user = user();
        when(userRepository.findByEmailIgnoreCase("nimal@example.test")).thenReturn(Optional.of(user));
        when(tokenService.createRefreshToken()).thenReturn("raw-token");
        when(tokenService.hashRefreshToken("raw-token")).thenReturn("token-hash");

        service(null).requestReset("  Nimal@Example.test ");

        verify(resetTokenRepository).invalidateAllByUserId(USER_ID, NOW);
        ArgumentCaptor<PasswordResetTokenEntity> saved = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(resetTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo("token-hash");
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(PasswordResetService.TOKEN_TTL));
        verify(events).publishEvent(new PasswordResetRequested(
                "nimal@example.test", "Nimal", "http://localhost:3000/reset-password?token=raw-token"));
    }

    @Test
    void forgotPasswordUsesStoredResetAddressWhenSet() {
        UserEntity user = user();
        user.setPasswordResetEmail("reset-inbox@example.test");
        when(userRepository.findByEmailIgnoreCase("nimal@example.test")).thenReturn(Optional.of(user));
        when(tokenService.createRefreshToken()).thenReturn("raw-token");

        service("other@example.test").requestReset("nimal@example.test");

        ArgumentCaptor<PasswordResetRequested> event = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().recipient()).isEqualTo("reset-inbox@example.test");
        assertThat(user.getPasswordResetEmail()).isEqualTo("reset-inbox@example.test");
    }

    @Test
    void forgotPasswordStoresDefaultResetAddressForAccountWithoutOne() {
        UserEntity user = user();
        when(userRepository.findByEmailIgnoreCase("nimal@example.test")).thenReturn(Optional.of(user));
        when(tokenService.createRefreshToken()).thenReturn("raw-token");

        service("demo-inbox@example.test").requestReset("nimal@example.test");

        assertThat(user.getPasswordResetEmail()).isEqualTo("demo-inbox@example.test");
        ArgumentCaptor<PasswordResetRequested> event = ArgumentCaptor.forClass(PasswordResetRequested.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().recipient()).isEqualTo("demo-inbox@example.test");
    }

    @Test
    void forgotPasswordForUnknownAccountSendsNothing() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.test")).thenReturn(Optional.empty());

        service(null).requestReset("nobody@example.test");

        verifyNoInteractions(resetTokenRepository, events);
    }

    @Test
    void forgotPasswordForDisabledAccountSendsNothing() {
        UserEntity user = user();
        user.disable();
        when(userRepository.findByEmailIgnoreCase("nimal@example.test")).thenReturn(Optional.of(user));

        service(null).requestReset("nimal@example.test");

        verifyNoInteractions(resetTokenRepository, events);
    }

    @Test
    void forgotPasswordSendsAtMostOneEmailPerMinute() {
        when(userRepository.findByEmailIgnoreCase("nimal@example.test")).thenReturn(Optional.of(user()));
        when(resetTokenRepository.existsUnusedSince(USER_ID, NOW.minus(PasswordResetService.RESEND_INTERVAL)))
                .thenReturn(true);

        service(null).requestReset("nimal@example.test");

        verify(resetTokenRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void resetSetsNewPasswordUsesUpLinkAndSignsOutEverywhere() {
        UserEntity user = user();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(user, "token-hash", NOW, NOW.plusSeconds(60));
        when(tokenService.hashRefreshToken("raw-token")).thenReturn("token-hash");
        when(resetTokenRepository.findByTokenHashForUpdate("token-hash")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("another-long-password")).thenReturn("new-hash");

        service(null).resetPassword("raw-token", "another-long-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        verify(resetTokenRepository).invalidateAllByUserId(USER_ID, NOW);
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID, NOW);
    }

    @Test
    void resetRejectsUnknownLink() {
        when(tokenService.hashRefreshToken("raw-token")).thenReturn("token-hash");
        when(resetTokenRepository.findByTokenHashForUpdate("token-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(null).resetPassword("raw-token", "another-long-password"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resetRejectsExpiredLink() {
        UserEntity user = user();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(user, "token-hash", NOW.minusSeconds(3600), NOW);
        when(tokenService.hashRefreshToken("raw-token")).thenReturn("token-hash");
        when(resetTokenRepository.findByTokenHashForUpdate("token-hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(null).resetPassword("raw-token", "another-long-password"))
                .isInstanceOf(ValidationException.class);
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void resetRejectsLinkThatWasAlreadyUsed() {
        UserEntity user = user();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(user, "token-hash", NOW, NOW.plusSeconds(60));
        token.markUsed(NOW);
        when(tokenService.hashRefreshToken("raw-token")).thenReturn("token-hash");
        when(resetTokenRepository.findByTokenHashForUpdate("token-hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(null).resetPassword("raw-token", "another-long-password"))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void resetRejectsLinkOfDisabledAccount() {
        UserEntity user = user();
        user.setAccountStatus(AccountStatus.DISABLED);
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(user, "token-hash", NOW, NOW.plusSeconds(60));
        when(tokenService.hashRefreshToken("raw-token")).thenReturn("token-hash");
        when(resetTokenRepository.findByTokenHashForUpdate("token-hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(null).resetPassword("raw-token", "another-long-password"))
                .isInstanceOf(ValidationException.class);
    }
}
