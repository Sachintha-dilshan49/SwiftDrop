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
import lk.swiftdrop.auth.account.OAuthAccountRepository;
import lk.swiftdrop.auth.account.RoleEntity;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.OAuthLoginCodeEntity;
import lk.swiftdrop.auth.token.OAuthLoginCodeRepository;
import lk.swiftdrop.auth.token.TokenResponse;
import lk.swiftdrop.common.error.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @Mock
    private OAuthLoginCodeRepository loginCodeRepository;

    @Mock
    private JwtTokenService tokenService;

    @Mock
    private AuthService authService;

    @Mock
    private OAuth2User googleUser;

    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        googleOAuthService = new GoogleOAuthService(
                userRepository,
                roleRepository,
                oauthAccountRepository,
                loginCodeRepository,
                tokenService,
                authService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsCustomerLinkAndShortLivedCodeForVerifiedGoogleProfile() {
        mockGoogleProfile(true);
        RoleEntity customer = new RoleEntity(RoleName.CUSTOMER);
        when(oauthAccountRepository.findByProviderAndProviderUserId("google", "google-user-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("demo@example.test")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customer));
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.createRefreshToken()).thenReturn("one-time-code");
        when(tokenService.hashRefreshToken("one-time-code")).thenReturn("hashed-code");

        String code = googleOAuthService.createLoginCode(googleUser);

        assertThat(code).isEqualTo("one-time-code");
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("demo@example.test");
        assertThat(userCaptor.getValue().getRoles()).extracting(RoleEntity::getName).containsExactly(RoleName.CUSTOMER);
        verify(oauthAccountRepository).save(any());
        ArgumentCaptor<OAuthLoginCodeEntity> codeCaptor = ArgumentCaptor.forClass(OAuthLoginCodeEntity.class);
        verify(loginCodeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void exchangesValidCodeOnceForSwiftDropTokens() {
        UserEntity user = new UserEntity("Demo Customer", "demo@example.test", null, null);
        OAuthLoginCodeEntity loginCode =
                new OAuthLoginCodeEntity(user, "hashed-code", NOW.plusSeconds(30));
        TokenResponse expected = new TokenResponse("access", NOW.plusSeconds(900), "refresh", null);
        when(tokenService.hashRefreshToken("one-time-code")).thenReturn("hashed-code");
        when(loginCodeRepository.findByCodeHashForUpdate("hashed-code")).thenReturn(Optional.of(loginCode));
        when(authService.issueForAuthenticatedUser(user)).thenReturn(expected);

        TokenResponse result = googleOAuthService.exchangeLoginCode("one-time-code");

        assertThat(result).isSameAs(expected);
        assertThat(loginCode.getConsumedAt()).isEqualTo(NOW);
        verify(authService).issueForAuthenticatedUser(user);
    }

    @Test
    void rejectsUnverifiedGoogleEmail() {
        mockGoogleProfile(false);

        assertThatThrownBy(() -> googleOAuthService.createLoginCode(googleUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Google account email must be verified.");
    }

    private void mockGoogleProfile(boolean verified) {
        when(googleUser.getAttribute("sub")).thenReturn("google-user-123");
        when(googleUser.getAttribute("email")).thenReturn("demo@example.test");
        when(googleUser.getAttribute("name")).thenReturn("Demo Customer");
        when(googleUser.getAttribute("email_verified")).thenReturn(verified);
    }

    @Test
    void rejectsExpiredOrAlreadyConsumedOneTimeCode() {
        UserEntity user = new UserEntity("Demo Customer", "demo@example.test", null, null);
        OAuthLoginCodeEntity expired = new OAuthLoginCodeEntity(user, "hashed-code", NOW);
        when(tokenService.hashRefreshToken(anyString())).thenReturn("hashed-code");
        when(loginCodeRepository.findByCodeHashForUpdate("hashed-code")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> googleOAuthService.exchangeLoginCode("one-time-code"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Google login code is invalid or expired.");
    }
}
