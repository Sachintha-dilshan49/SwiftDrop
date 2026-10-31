package lk.swiftdrop.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.auth.service.GoogleOAuthService;
import lk.swiftdrop.auth.token.TokenResponse;
import lk.swiftdrop.auth.token.TokenResponse.UserSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Test
    void delegatesSignupAndLoginRequests() {
        AuthController controller = new AuthController(authService, googleOAuthService);
        SignupRequest signup = new SignupRequest("Demo", "demo@example.test", null, "long-enough-password");
        LoginRequest login = new LoginRequest("demo@example.test", "long-enough-password");
        TokenResponse tokens = tokenResponse();
        when(authService.signup(signup)).thenReturn(tokens);
        when(authService.login(login)).thenReturn(tokens);

        var signupResponse = controller.signup(signup);
        assertThat(signupResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(signupResponse.getBody()).isSameAs(tokens);
        assertThat(controller.login(login)).isSameAs(tokens);
        verify(authService).signup(signup);
        verify(authService).login(login);
    }

    @Test
    void delegatesRefreshLogoutAndGoogleCodeExchange() {
        AuthController controller = new AuthController(authService, googleOAuthService);
        RefreshRequest refresh = new RefreshRequest("refresh");
        LogoutRequest logout = new LogoutRequest("refresh");
        OAuthTokenRequest oauth = new OAuthTokenRequest("code");
        TokenResponse tokens = tokenResponse();
        when(authService.refresh("refresh")).thenReturn(tokens);
        when(googleOAuthService.exchangeLoginCode("code")).thenReturn(tokens);

        assertThat(controller.refresh(refresh)).isSameAs(tokens);
        assertThat(controller.logout(logout).getStatusCode().value()).isEqualTo(204);
        assertThat(controller.exchangeGoogleCode(oauth)).isSameAs(tokens);
        verify(authService).refresh("refresh");
        verify(authService).logout("refresh");
        verify(googleOAuthService).exchangeLoginCode("code");
    }

    @Test
    void requestAndResponseDtosExposeExpectedValues() {
        SignupRequest signup = new SignupRequest("Demo", "demo@example.test", "0771234567", "long-enough-password");
        LoginRequest login = new LoginRequest("demo@example.test", "password");
        assertThat(signup.fullName()).isEqualTo("Demo");
        assertThat(signup.email()).isEqualTo("demo@example.test");
        assertThat(signup.mobileNumber()).isEqualTo("0771234567");
        assertThat(signup.password()).isEqualTo("long-enough-password");
        assertThat(login.emailOrMobile()).isEqualTo("demo@example.test");
        assertThat(login.password()).isEqualTo("password");
        assertThat(new RefreshRequest("refresh").refreshToken()).isEqualTo("refresh");
        assertThat(new LogoutRequest("refresh").refreshToken()).isEqualTo("refresh");
        assertThat(new OAuthTokenRequest("code").code()).isEqualTo("code");

        TokenResponse response = tokenResponse();
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessTokenExpiresAt()).isEqualTo(Instant.parse("2026-10-30T00:15:00Z"));
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().id()).isNotNull();
        assertThat(response.user().email()).isEqualTo("demo@example.test");
    }

    private static TokenResponse tokenResponse() {
        return new TokenResponse(
                "access",
                Instant.parse("2026-10-30T00:15:00Z"),
                "refresh",
                new UserSummary(UUID.randomUUID(), "Demo", "demo@example.test", java.util.List.of("CUSTOMER")));
    }
}
