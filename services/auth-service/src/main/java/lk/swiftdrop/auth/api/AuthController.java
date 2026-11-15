package lk.swiftdrop.auth.api;

import jakarta.validation.Valid;
import lk.swiftdrop.auth.reset.PasswordResetService;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.auth.service.GoogleOAuthService;
import lk.swiftdrop.auth.token.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, GoogleOAuthService googleOAuthService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/token")
    public TokenResponse exchangeGoogleCode(@Valid @RequestBody OAuthTokenRequest request) {
        return googleOAuthService.exchangeLoginCode(request.code());
    }

    /** Always 202, whether or not the account exists. */
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
