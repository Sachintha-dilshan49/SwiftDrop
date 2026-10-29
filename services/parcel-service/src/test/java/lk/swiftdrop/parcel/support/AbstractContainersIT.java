package lk.swiftdrop.parcel.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Base for Spring integration tests: the full application against real PostgreSQL and RabbitMQ.
 *
 * <ul>
 *   <li>Subclasses without extra bean overrides share one Spring context.</li>
 *   <li>The scheduled outbox relay effectively never fires on its own; tests call it explicitly.</li>
 * </ul>
 */
@SpringBootTest(properties = "swiftdrop.outbox.poll-interval-ms=3600000")
public abstract class AbstractContainersIT {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = SharedContainers.POSTGRES;

    @ServiceConnection
    static final RabbitMQContainer RABBIT = SharedContainers.RABBIT;
}
