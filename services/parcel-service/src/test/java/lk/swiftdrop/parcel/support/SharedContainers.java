package lk.swiftdrop.parcel.support;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * One PostgreSQL and one RabbitMQ container for every integration test in the JVM.
 *
 * <p>They are started once and removed by Testcontainers after the JVM exits, so cached Spring
 * contexts never outlive their broker or database. Docker is required: without it these tests
 * fail instead of being skipped.
 */
public final class SharedContainers {

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    public static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    private SharedContainers() {
    }
}
