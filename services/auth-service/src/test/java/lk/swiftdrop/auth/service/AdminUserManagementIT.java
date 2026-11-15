package lk.swiftdrop.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.api.AdminUserUpdateRequest;
import lk.swiftdrop.auth.token.RefreshTokenEntity;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class AdminUserManagementIT {

    private static final String TEST_PRIVATE_KEY_BASE64 = testPrivateKey();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AdminUserService adminUserService;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.jwt.private-key-base64", () -> TEST_PRIVATE_KEY_BASE64);
        registry.add("swiftdrop.internal.token", () -> "auth-service-integration-test-token");
    }

    @Test
    void persistsRoleAndAccountStatusChangesAndRevokesRefreshTokens() {
        UserEntity user = new UserEntity("Demo User", "demo@example.test", "0771234567", "hash");
        user.addRole(roleRepository.findByName(RoleName.CUSTOMER).orElseThrow());
        UserEntity savedUser = userRepository.saveAndFlush(user);
        refreshTokenRepository.saveAndFlush(new RefreshTokenEntity(
                savedUser,
                "refresh-token-hash",
                Instant.now().plusSeconds(60 * 60 * 24)));

        adminUserService.updateUser(
                savedUser.getId(),
                new AdminUserUpdateRequest(Set.of(RoleName.MERCHANT), AccountStatus.DISABLED));

        UserEntity updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getRoles()).extracting(role -> role.getName())
                .containsExactly(RoleName.MERCHANT);
        assertThat(updatedUser.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(refreshTokenRepository.findByTokenHash("refresh-token-hash").orElseThrow().getRevokedAt())
                .isNotNull();
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
