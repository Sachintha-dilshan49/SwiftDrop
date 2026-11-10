package lk.swiftdrop.pricing.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL container for every integration test in the JVM, started once and removed by
 * Testcontainers after the JVM exits. Docker is required: without it these tests fail instead of
 * being skipped.
 */
public final class SharedPostgres {

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private SharedPostgres() {
    }
}
