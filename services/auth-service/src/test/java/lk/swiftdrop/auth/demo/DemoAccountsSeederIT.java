package lk.swiftdrop.auth.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** On a fresh database with AUTH_DEMO_ACCOUNTS_PASSWORD set, one staff account per role can sign in. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DemoAccountsSeederIT {

    private static final String PASSWORD = "Demo-password-2026";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoAccountsSeeder seeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.jwt.private-key-base64", DemoAccountsSeederIT::testPrivateKey);
        registry.add("swiftdrop.internal.token", () -> "auth-service-integration-test-token");
        registry.add("swiftdrop.demo-accounts.password", () -> PASSWORD);
    }

    @Test
    void createsOneStaffAccountPerRoleWithThePassword() {
        for (DemoAccountsSeeder.DemoAccount account : DemoAccountsSeeder.ACCOUNTS) {
            UserEntity user = userRepository.findByEmailIgnoreCase(account.email()).orElseThrow();
            assertThat(user.getRoles()).extracting(role -> role.getName()).containsExactly(account.role());
            assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        }
        assertThat(DemoAccountsSeeder.ACCOUNTS).extracting(DemoAccountsSeeder.DemoAccount::role)
                .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.DISPATCHER, RoleName.COURIER);
    }

    @Test
    void runningAgainCreatesNoDuplicates() {
        seeder.run(null);

        assertThat(userRepository.findAll()).filteredOn(u -> u.getEmail().startsWith("demo."))
                .hasSize(DemoAccountsSeeder.ACCOUNTS.size());
    }

    @Test
    void profileTablesAreGone() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);

        assertThat(tables).doesNotContain("merchant_profiles", "courier_profiles").contains("users", "user_roles");
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
