package lk.swiftdrop.notification;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("notification_db")
                    .withUsername("test")
                    .withPassword("test");

    static final RabbitMQContainer rabbit =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        postgres.start();
        rabbit.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        // Disable real JWT verification
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:0/.well-known/jwks.json");

        // Mail — dummy port
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "0");
        registry.add("management.health.mail.enabled", () -> "false");

        // Service URLs — unreachable on purpose (overridden per test if needed)
        registry.add("swiftdrop.services.auth-url", () -> "http://localhost:0");
        registry.add("swiftdrop.services.merchant-url", () -> "http://localhost:0");
    }
}

