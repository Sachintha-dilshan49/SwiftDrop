package lk.swiftdrop.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.api.LoginRequest;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.auth.token.RefreshTokenEntity;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.UnauthorizedException;
import lk.swiftdrop.common.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PasswordResetIT {

    private static final String TEST_PRIVATE_KEY_BASE64 = testPrivateKey();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean private JavaMailSender mailSender;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private AuthService authService;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.jwt.private-key-base64", () -> TEST_PRIVATE_KEY_BASE64);
        registry.add("swiftdrop.internal.token", () -> "auth-service-integration-test-token");
        registry.add("swiftdrop.password-reset.reset-url", () -> "http://localhost:3000/reset-password");
    }

    @Test
    void emailedLinkSetsNewPasswordOnceAndSignsOutOtherDevices() {
        UserEntity user = new UserEntity("Reset User", "reset.user@example.test", null,
                passwordEncoder.encode("original-password-1"));
        user.setPasswordResetEmail("reset-inbox@example.test");
        user.addRole(roleRepository.findByName(RoleName.CUSTOMER).orElseThrow());
        user = userRepository.saveAndFlush(user);
        refreshTokenRepository.saveAndFlush(new RefreshTokenEntity(user, "reset-it-refresh",
                Instant.now().plusSeconds(3600)));

        passwordResetService.requestReset("Reset.User@example.test");

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("reset-inbox@example.test");
        String text = mail.getValue().getText();
        int start = text.indexOf("token=") + "token=".length();
        String token = URLDecoder.decode(text.substring(start, text.indexOf('\n', start)).trim(), StandardCharsets.UTF_8);

        passwordResetService.resetPassword(token, "brand-new-password-2");

        assertThat(authService.login(new LoginRequest("reset.user@example.test", "brand-new-password-2"))
                .accessToken()).isNotBlank();
        assertThatThrownBy(() -> authService.login(new LoginRequest("reset.user@example.test", "original-password-1")))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(refreshTokenRepository.findByTokenHash("reset-it-refresh").orElseThrow().getRevokedAt())
                .isNotNull();
        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "third-password-333"))
                .isInstanceOf(ValidationException.class);
    }

    private static String testPrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Could not generate test signing key.", exception);
        }
    }
}
