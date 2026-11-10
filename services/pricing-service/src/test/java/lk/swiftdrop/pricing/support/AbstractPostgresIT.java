package lk.swiftdrop.pricing.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Base for Spring integration tests: the full application on a random port against a real PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = SharedPostgres.POSTGRES;
}
